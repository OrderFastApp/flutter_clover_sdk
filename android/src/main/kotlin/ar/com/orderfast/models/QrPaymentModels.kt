package ar.com.orderfast.models

/**
 * Modelos de datos para pagos con QR (Pagos QR de Fiserv, v3.1)
 */

data class QrPaymentRequest(
    val amount: Long,
    /** Se envía como `externalPaymentId` (campo reservado para el ISV). */
    val externalId: String,
    /** Id de una orden de Clover existente (opcional). */
    val orderId: String? = null,
    /**
     * Referencia propia de la app (p. ej. el id de la orden de OrderFast). Se
     * persiste con el pago y vuelve en el resultado huérfano para poder
     * completar la orden aunque el proceso haya muerto.
     */
    val reference: String? = null
)

data class QrPaymentResponse(
    val success: Boolean,
    val result: String? = null,
    val reason: String? = null,
    val message: String? = null,
    val payment: PaymentInfo? = null
)
