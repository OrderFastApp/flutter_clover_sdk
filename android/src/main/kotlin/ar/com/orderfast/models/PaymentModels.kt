package ar.com.orderfast.models

/**
 * Modelos de datos para el SDK de Clover
 */

data class PaymentRequest(
    val amount: Long,
    val externalId: String
)

data class PaymentResponse(
    val success: Boolean,
    val result: String? = null,
    val reason: String? = null,
    val message: String? = null,
    val payment: PaymentInfo? = null
)

data class PaymentInfo(
    val id: String? = null,
    val orderId: String? = null,
    val externalPaymentId: String? = null,
    val amount: Long? = null,
    val tipAmount: Long? = null,
    val taxAmount: Long? = null,
    val result: String? = null
)

data class DeviceStatus(
    val connected: Boolean,
    val message: String
)
