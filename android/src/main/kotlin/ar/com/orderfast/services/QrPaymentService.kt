package ar.com.orderfast.services

import android.app.Activity
import android.content.Intent
import android.util.Log
import ar.com.orderfast.models.QrPaymentRequest
import ar.com.orderfast.models.QrPaymentResponse
import com.clover.common2.payments.PayIntent
import com.clover.sdk.v1.Intents

/**
 * Servicio para manejar pagos con QR
 * Muestra un código QR que el cliente puede escanear para realizar el pago
 */
class QrPaymentService(private val context: android.content.Context) {

    companion object {
        private const val TAG = "QrPaymentService"
    }

    /**
     * Presenta un código QR para que el cliente escanee y realice el pago
     * @param activity La actividad actual
     * @param request Información del pago QR
     * @param callback Callback cuando se completa o falla el pago
     */
    fun presentQrCode(
        activity: Activity,
        request: QrPaymentRequest,
        callback: (QrPaymentResponse) -> Unit
    ) {
        try {
            // Crear PayIntent con isPresentQrcOnly = true
            val payIntent = PayIntent.Builder()
                .amount(request.amount)
                .isPresentQrcOnly(true)
                .apply {
                    // Si hay orderId, agregarlo
                    request.orderId?.let { orderId(it) }
                }
                .build()

            // Crear Intent para lanzar la actividad de pago
            val intent = Intent(Intents.ACTION_CUSTOMER_TENDER).apply {
                putExtra(Intents.EXTRA_AMOUNT, request.amount)
                putExtra(Intents.EXTRA_PRESENT_QRC_ONLY, true)
                putExtra(Intents.EXTRA_PAYMENT, payIntent)
                
                // Agregar externalId si es necesario
                request.externalId.let { 
                    putExtra(Intents.EXTRA_ORDER_ID, it)
                }
                
                // Agregar orderId si está presente
                request.orderId?.let {
                    putExtra(Intents.EXTRA_CLOVER_ORDER_ID, it)
                }
            }

            // Lanzar la actividad de pago
            activity.startActivity(intent)
            
            Log.d(TAG, "QR Code presentado para pago: amount=${request.amount}, externalId=${request.externalId}")
            
            // Nota: La respuesta del pago llegará a través de un broadcast receiver
            // o callback cuando el cliente complete el pago escaneando el QR
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al presentar QR Code", e)
            callback(QrPaymentResponse(
                success = false,
                reason = "EXCEPTION",
                message = e.message ?: "Error desconocido al presentar QR"
            ))
        }
    }

    /**
     * Verifica si el servicio está disponible
     */
    fun isAvailable(): Boolean {
        val intent = Intent(Intents.ACTION_PAY)
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo != null
    }
}
