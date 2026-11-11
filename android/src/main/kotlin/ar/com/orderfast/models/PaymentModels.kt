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
    /** Id de la orden de Clover (`payment.order.id`). */
    val orderId: String? = null,
    val externalPaymentId: String? = null,
    /**
     * `payment.externalReferenceId`. En pagos QR es el **ID del QR**, no una
     * orden.
     */
    val externalReferenceId: String? = null,
    val amount: Long? = null,
    val tipAmount: Long? = null,
    val taxAmount: Long? = null,
    val result: String? = null,
    val createdTime: Long? = null,
    val offline: Boolean? = null,
    val note: String? = null,
    /**
     * `payment.attributes`. En Pagos QR 3.1: "ID QR", "ID Autorización",
     * "Lote", "Cupón", "Billetera", "Medio", "Cuotas", "Bin Tarjeta",
     * "ID Billetera", "note". "Cuotas" y "Bin Tarjeta" no vienen en
     * transferencias PCT.
     */
    val attributes: Map<String, String>? = null,
    val cardTransaction: CardTransactionInfo? = null,
    val tender: TenderInfo? = null,
    val transactionInfo: TransactionInfoData? = null
)

data class CardTransactionInfo(
    val authCode: String? = null,
    val cardType: String? = null,
    val currency: String? = null,
    /** No viene en transferencias PCT. */
    val last4: String? = null,
    val terminalId: String? = null
)

data class TenderInfo(
    val id: String? = null,
    val label: String? = null,
    val labelKey: String? = null
)

data class TransactionInfoData(
    val cardTypeLabel: String? = null,
    val entryType: String? = null,
    val batchNumber: String? = null,
    val receiptNumber: String? = null,
    val installmentsQuantity: Int? = null
)

data class DeviceStatus(
    val connected: Boolean,
    val message: String
)
