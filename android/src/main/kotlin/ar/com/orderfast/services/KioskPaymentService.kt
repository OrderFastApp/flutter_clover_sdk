package ar.com.orderfast.services

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import com.clover.sdk.util.CloverAuth
import com.clover.sdk.v1.Intents
import ar.com.orderfast.mappers.PaymentMapper
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.models.PaymentResponse
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.OfflineOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.ReceiptOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.SignatureOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.TipOptions
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cobro con tarjeta en Clover Kiosk vía [KioskPayRequestIntentBuilder].
 *
 * A diferencia de `PaymentConnector.sale`, no hay callback: se lanza una
 * Activity de Clover con `startActivityForResult` y el resultado llega a
 * [handleActivityResult]. Solo existe en firmware Kiosk; en otros modelos
 * [isAvailable] devuelve false y hay que usar `sale`.
 */
class KioskPaymentService(private val context: Context) {

    companion object {
        private const val TAG = "KioskPaymentService"
        const val REQUEST_CODE = 0x4B50 // "KP"

        private const val HANDLER_PACKAGE = "com.clover.payment.builder.pay"
        private const val HANDLER_CLASS =
            "com.clover.payment.builder.pay.handler.KioskPayRequestHandler"
    }

    private var pendingCallback: ((PaymentResponse) -> Unit)? = null

    val isInFlight: Boolean get() = pendingCallback != null

    /** true si el handler Kiosk de Clover está instalado en este equipo. */
    fun isAvailable(): Boolean {
        val intent = Intent().setComponent(ComponentName(HANDLER_PACKAGE, HANDLER_CLASS))
        return context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    fun pay(
        activity: Activity,
        request: PaymentRequest,
        callback: (PaymentResponse) -> Unit
    ) {
        if (pendingCallback != null) {
            Log.w(
                TAG,
                "BUSY: nuevo cobro ${request.externalId} con otro pendiente hace " +
                    "${SystemClock.elapsedRealtime() - launchedAtMs}ms"
            )
            callback(failure("BUSY", "Ya hay un cobro Kiosk en curso"))
            return
        }

        // Se reserva el cobro ya: el chequeo de auth corre fuera del hilo
        // principal y un segundo cobro no debe colarse mientras tanto.
        pendingCallback = callback
        launchedAtMs = SystemClock.elapsedRealtime()
        authExecutor.execute {
            // Solo diagnóstico, NO bloquea: sin token de app el handler de Clover
            // apenas avisa ("No token found for <paquete>") y sigue el flujo (p. ej.
            // en sandbox / apps instaladas por sideload). Además, esta llamada deja
            // el token listo para el `CloverAuth` de `build()` (que corre en main).
            val authError = validateAuth()
            if (authError != null) {
                Log.w(TAG, "⚠️ auth de Clover sin credenciales, se lanza igual: $authError")
            }
            handler.post {
                if (pendingCallback == null) return@post // dispose() mientras tanto
                launch(activity, request, callback)
            }
        }
    }

    /**
     * `KioskPayRequestIntentBuilder.build` pide el token con `CloverAuth` y
     * traga cualquier fallo: si no hay credenciales el intent sale sin `appId`.
     * Se consulta antes, en un hilo de fondo (puede hacer red), con un reintento
     * forzando la validación contra la nube de Clover.
     *
     * @return null si hay credenciales; si no, el motivo (solo para el log).
     */
    private fun validateAuth(): String? {
        var lastError = "Sin credenciales de Clover"
        for (force in listOf(false, true)) {
            try {
                val auth = CloverAuth.authenticate(context, force, 20L, TimeUnit.SECONDS)
                val hasAppId = !auth?.appId.isNullOrEmpty()
                val hasToken = !auth?.authToken.isNullOrEmpty()
                if (hasAppId && hasToken) {
                    if (force) Log.i(TAG, "auth OK tras reintento con forceValidate")
                    return null
                }
                lastError = auth?.errorMessage ?: lastError
                Log.w(
                    TAG,
                    "auth inválida (forceValidate=$force) appId=$hasAppId token=$hasToken " +
                        "error=${auth?.errorMessage} merchantId=${auth?.merchantId}"
                )
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
                Log.w(TAG, "auth falló (forceValidate=$force): ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        return lastError
    }

    private fun launch(
        activity: Activity,
        request: PaymentRequest,
        callback: (PaymentResponse) -> Unit
    ) {
        try {
            val intent = KioskPayRequestIntentBuilder(request.amount, request.externalId)
                // Total fijo: sin pagos parciales (default de Clover: true).
                .allowPartialPayments(false)
                .tipOptions(TipOptions.Disable())
                // El ticket lo imprime OrderFast, no Clover. Se usa Instance(false, ...)
                // y no SkipReceiptSelection(): ese deja cloverShouldHandleReceipts=true
                // (EXTRA_REMOTE_RECEIPTS=false) y Clover imprime su recibo igual.
                .receiptOptions(
                    ReceiptOptions.Instance(
                        false,
                        ReceiptOptions.SmsReceiptOption.Disable(),
                        ReceiptOptions.EmailReceiptOption.Disable(),
                        ReceiptOptions.PrintReceiptOption.Disable(),
                        ReceiptOptions.NoReceiptOption.Disable()
                    )
                )
                .signatureOptions(SignatureOptions.Disable())
                .offlineOptions(OfflineOptions.Instance(false, false, false))
                .build(activity)

            launchedAtMs = SystemClock.elapsedRealtime()
            lastActivity = java.lang.ref.WeakReference(activity)
            Log.i(
                TAG,
                "🚀 lanzando Kiosk pay · externalId=${request.externalId} monto=${request.amount} " +
                    "· activityDisplay=${displayIdOf(activity)} " +
                    "· component=${intent.component?.flattenToShortString()} flags=0x${intent.flags.toString(16)} " +
                    "· requestId=${intent.extras?.get(Intents.EXTRA_REQUEST_ID)}"
            )
            logFocus(activity, "pre-launch")
            activity.startActivityForResult(intent, REQUEST_CODE)
            scheduleFocusProbes(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar Kiosk pay", e)
            pendingCallback = null
            callback(failure("EXCEPTION", e.message))
        }
    }

    /** @return true si el resultado era de este servicio. */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false
        val callback = pendingCallback ?: run {
            Log.w(TAG, "resultado Kiosk sin cobro pendiente (code=$resultCode): ignorado")
            return true
        }
        pendingCallback = null
        cancelFocusProbes()
        schedulePostResultProbes()

        val elapsedMs = SystemClock.elapsedRealtime() - launchedAtMs
        val failureMessage = data?.getStringExtra(KioskPayRequestIntentBuilder.Response.FAILURE_MESSAGE)
        val payment = data?.let { extractPayment(it) }
        Log.i(
            TAG,
            "🏁 resultado Kiosk pay tras ${elapsedMs}ms · code=${resultCodeName(resultCode)} " +
                "payment=${payment?.id} result=${payment?.result} failure=$failureMessage " +
                "· extras=${describeExtras(data)}"
        )

        val seconds = "%.1f".format(elapsedMs / 1000.0)
        val response = when {
            resultCode == Activity.RESULT_CANCELED ->
                failure("CANCEL", (failureMessage ?: "Pago cancelado") + " (tras ${seconds}s)")
            resultCode != Activity.RESULT_OK || failureMessage != null ->
                failure("FAIL", (failureMessage ?: "El pago no se completó") + " (tras ${seconds}s)")
            else -> PaymentResponse(
                success = true,
                result = payment?.result?.name ?: "SUCCESS",
                payment = payment?.let { PaymentMapper.toPaymentInfo(it) }
            )
        }
        callback(response)
        return true
    }

    /** Cancela el callback pendiente (p. ej. al hacer dispose del plugin). */
    fun dispose() {
        if (pendingCallback != null) {
            Log.w(TAG, "dispose con un cobro Kiosk pendiente: se descarta el callback")
        }
        pendingCallback = null
        cancelFocusProbes()
    }

    // --- Diagnóstico -------------------------------------------------------

    private var launchedAtMs = 0L
    private var lastActivity: java.lang.ref.WeakReference<Activity>? = null
    private val authExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val probes = mutableListOf<Runnable>()

    /** Sondas de foco a 1 s, 3 s y 6 s del lanzamiento: ¿quién está arriba? */
    private fun scheduleFocusProbes(activity: Activity) {
        cancelFocusProbes()
        for (delayMs in longArrayOf(1_000L, 3_000L, 6_000L)) {
            val probe = Runnable {
                if (pendingCallback != null) logFocus(activity, "+${delayMs}ms")
            }
            probes += probe
            handler.postDelayed(probe, delayMs)
        }
    }

    /**
     * Sondas tras el resultado (1, 3, 6, 10 y 15 s): ¿cuándo termina Clover su
     * actividad final en la pantalla cliente y quién queda arriba? Diagnóstico
     * para ajustar el recovery diferido.
     */
    private fun schedulePostResultProbes() {
        val activity = lastActivity?.get() ?: return
        for (delayMs in longArrayOf(1_000L, 3_000L, 6_000L, 10_000L, 15_000L)) {
            val probe = Runnable { logFocus(activity, "post-resultado +${delayMs}ms") }
            probes += probe
            handler.postDelayed(probe, delayMs)
        }
    }

    private fun cancelFocusProbes() {
        probes.forEach { handler.removeCallbacks(it) }
        probes.clear()
    }

    private fun logFocus(activity: Activity, label: String) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val top = am.getRunningTasks(1).firstOrNull()?.topActivity
            Log.i(
                TAG,
                "🔎 foco [$label] top=${top?.flattenToShortString() ?: "?"} " +
                    "· activityDisplay=${displayIdOf(activity)} " +
                    "· activityFinishing=${activity.isFinishing}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "🔎 foco [$label] no disponible: ${e.message}")
        }
    }

    private fun displayIdOf(activity: Activity): Int =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.displayId ?: -1
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.displayId
            }
        } catch (e: Exception) {
            -1
        }

    private fun resultCodeName(code: Int) = when (code) {
        Activity.RESULT_OK -> "RESULT_OK"
        Activity.RESULT_CANCELED -> "RESULT_CANCELED"
        else -> "RESULT_$code"
    }

    /** Claves y tipos de los extras (sin valores: pueden traer datos del pago). */
    private fun describeExtras(data: Intent?): String {
        val extras = data?.extras ?: return "none"
        return try {
            extras.keySet().joinToString(prefix = "{", postfix = "}") { key ->
                @Suppress("DEPRECATION")
                "$key:${extras.get(key)?.javaClass?.simpleName ?: "null"}"
            }
        } catch (e: Exception) {
            "error(${e.message})"
        }
    }

    private fun failure(reason: String, message: String?) =
        PaymentResponse(success = false, reason = reason, message = message)

    /**
     * El formato de `EXTRA_PAYMENTS` no está documentado: se acepta una lista
     * parcelable, un `Payment` suelto o un `Serializable` con pagos.
     */
    @Suppress("DEPRECATION")
    private fun extractPayment(data: Intent): Payment? {
        val key = KioskPayRequestIntentBuilder.Response.PAYMENTS
        val raw: Any? = data.extras?.get(key)
        val candidates: List<Any?> = when (raw) {
            is Collection<*> -> raw.toList()
            is Array<*> -> raw.toList()
            is Parcelable -> listOf(raw)
            else -> listOf(raw)
        }
        return candidates.filterIsInstance<Payment>().firstOrNull()
    }
}
