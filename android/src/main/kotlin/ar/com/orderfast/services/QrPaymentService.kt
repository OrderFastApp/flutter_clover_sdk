package ar.com.orderfast.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import ar.com.orderfast.mappers.PaymentMapper
import ar.com.orderfast.models.QrPaymentRequest
import ar.com.orderfast.models.QrPaymentResponse
import com.clover.common2.payments.PayIntent
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Payment

/**
 * Cobro con QR (Pagos QR de Fiserv) vía la pantalla de pago de Clover.
 *
 * Lanza `ACTION_SECURE_PAY` con `EXTRA_PRESENT_QRC_ONLY` (Clover muestra el QR
 * como único medio de pago) usando `startActivityForResult`: el resultado llega
 * a [handleActivityResult] con el `Payment` creado en `EXTRA_PAYMENT`.
 *
 * Nota: `ACTION_CUSTOMER_TENDER` NO sirve para esto; es el intent con el que
 * Clover abre la activity de *tender extensible* de una app, no uno que una app
 * deba lanzar.
 */
class QrPaymentService(private val context: Context) {

    companion object {
        private const val TAG = "QrPaymentService"
        const val REQUEST_CODE = 0x5152 // "QR"
    }

    private var pendingCallback: ((QrPaymentResponse) -> Unit)? = null

    val isInFlight: Boolean get() = pendingCallback != null

    /** true si hay una activity de Clover que atienda el pago seguro. */
    fun isAvailable(): Boolean {
        val intent = Intent(Intents.ACTION_SECURE_PAY)
        return context.packageManager.resolveActivity(intent, 0) != null
    }

    /**
     * Presenta el QR y espera el resultado.
     *
     * [callback] se invoca **una sola vez**: con el pago aprobado, la
     * cancelación, un error o `BUSY` si ya hay un cobro QR en curso.
     */
    fun presentQrCode(
        activity: Activity,
        request: QrPaymentRequest,
        callback: (QrPaymentResponse) -> Unit
    ) {
        if (pendingCallback != null) {
            Log.w(TAG, "BUSY: nuevo QR ${request.externalId} con otro pendiente")
            callback(failure("BUSY", "Ya hay un pago QR en curso"))
            return
        }

        try {
            val payIntent = PayIntent.Builder()
                .action(Intents.ACTION_SECURE_PAY)
                .amount(request.amount)
                .externalPaymentId(request.externalId)
                .isPresentQrcOnly(true)
                .apply { request.orderId?.let { orderId(it) } }
                .build()

            // `addTo` vuelca amount, orderId, externalPaymentId y
            // EXTRA_PRESENT_QRC_ONLY: no hace falta repetirlos a mano.
            val intent = Intent(Intents.ACTION_SECURE_PAY)
            payIntent.addTo(intent)

            pendingCallback = callback
            activity.startActivityForResult(intent, REQUEST_CODE)
            Log.d(
                TAG,
                "QR presentado: amount=${request.amount}, externalPaymentId=${request.externalId}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al presentar QR Code", e)
            pendingCallback = null
            callback(failure("EXCEPTION", e.message ?: "Error desconocido al presentar QR"))
        }
    }

    /** @return true si el resultado era de este servicio. */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE) return false
        val callback = pendingCallback ?: run {
            Log.w(TAG, "resultado QR sin cobro pendiente (code=$resultCode): ignorado")
            return true
        }
        pendingCallback = null

        val failureMessage = data?.getStringExtra(Intents.EXTRA_FAILURE_MESSAGE)
        val payment = data?.let { extractPayment(it) }
        Log.i(
            TAG,
            "resultado QR · code=$resultCode payment=${payment?.id} " +
                "result=${payment?.result} failure=$failureMessage"
        )

        val response = when {
            resultCode == Activity.RESULT_CANCELED ->
                failure("CANCEL", failureMessage ?: "Pago QR cancelado")
            resultCode != Activity.RESULT_OK || failureMessage != null ->
                failure("FAIL", failureMessage ?: "El pago QR no se completó")
            // RESULT_OK sin `Payment`: no hay con qué conciliar, no se da por
            // cobrado.
            payment == null ->
                failure("NO_PAYMENT", "Clover no devolvió el pago del QR")
            else -> QrPaymentResponse(
                success = true,
                result = payment.result?.name ?: "SUCCESS",
                payment = PaymentMapper.toPaymentInfo(payment)
            )
        }
        callback(response)
        return true
    }

    /** Descarta el callback pendiente (p. ej. al hacer dispose del plugin). */
    fun dispose() {
        if (pendingCallback != null) {
            Log.w(TAG, "dispose con un pago QR pendiente: se descarta el callback")
        }
        pendingCallback = null
    }

    private fun failure(reason: String, message: String?) =
        QrPaymentResponse(success = false, reason = reason, message = message)

    @Suppress("DEPRECATION")
    private fun extractPayment(data: Intent): Payment? {
        data.setExtrasClassLoader(Payment::class.java.classLoader)
        val raw: Any? = data.extras?.get(Intents.EXTRA_PAYMENT)
        val candidates: List<Any?> = when (raw) {
            is Collection<*> -> raw.toList()
            is Array<*> -> raw.toList()
            is Parcelable -> listOf(raw)
            else -> listOf(raw)
        }
        return candidates.filterIsInstance<Payment>().firstOrNull()
    }
}
