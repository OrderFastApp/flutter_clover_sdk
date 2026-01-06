package ar.com.orderfast.models

/**
 * Modelos de datos para pagos con QR
 */

data class QrPaymentRequest(
    val amount: Long,
    val externalId: String,
    val orderId: String? = null
)

data class QrPaymentResponse(
    val success: Boolean,
    val result: String? = null,
    val reason: String? = null,
    val message: String? = null,
    val qrCodeData: String? = null,
    val payment: PaymentInfo? = null
)
