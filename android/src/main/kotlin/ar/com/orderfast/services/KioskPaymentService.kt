package ar.com.orderfast.services

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Parcelable
import android.util.Log
import ar.com.orderfast.mappers.PaymentMapper
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.models.PaymentResponse
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.OfflineOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.ReceiptOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.SignatureOptions
import com.clover.sdk.v3.payments.api.KioskPayRequestIntentBuilder.TipOptions

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
            callback(failure("BUSY", "Ya hay un cobro Kiosk en curso"))
            return
        }
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

            pendingCallback = callback
            activity.startActivityForResult(intent, REQUEST_CODE)
            Log.d(TAG, "Kiosk pay lanzado: ${request.externalId}, monto: ${request.amount}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar Kiosk pay", e)
            pendingCallback = null
            callback(failure("EXCEPTION", e.message))
        }
    }

    /** @return true si el resultado era de este servicio. */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false
        val callback = pendingCallback ?: return true
        pendingCallback = null

        val failureMessage = data?.getStringExtra(KioskPayRequestIntentBuilder.Response.FAILURE_MESSAGE)
        val payment = data?.let { extractPayment(it) }
        Log.d(
            TAG,
            "Kiosk pay result: code=$resultCode payment=${payment?.id} " +
                "result=${payment?.result} failure=$failureMessage"
        )

        val response = when {
            resultCode == Activity.RESULT_CANCELED ->
                failure("CANCEL", failureMessage ?: "Pago cancelado")
            resultCode != Activity.RESULT_OK || failureMessage != null ->
                failure("FAIL", failureMessage ?: "El pago no se completó")
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
        pendingCallback = null
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
