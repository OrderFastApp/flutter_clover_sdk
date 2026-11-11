package ar.com.orderfast.mappers

import ar.com.orderfast.models.CardTransactionInfo
import ar.com.orderfast.models.PaymentInfo
import ar.com.orderfast.models.PaymentResponse
import ar.com.orderfast.models.TenderInfo
import ar.com.orderfast.models.TransactionInfoData
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.remotepay.SaleResponse

/**
 * Mapper para convertir objetos de Clover SDK a modelos de la aplicación
 */
object PaymentMapper {

    fun toPaymentResponse(response: SaleResponse): PaymentResponse {
        return PaymentResponse(
            success = response.success ?: false,
            result = response.result?.name,
            reason = response.reason,
            message = response.message,
            payment = response.payment?.let { toPaymentInfo(it) }
        )
    }

    fun toPaymentInfo(payment: Payment): PaymentInfo {
        return PaymentInfo(
            id = payment.id,
            // `externalReferenceId` NO es la orden (en QR es el ID del QR): la
            // orden de Clover está en `payment.order.id`.
            orderId = payment.order?.id,
            externalPaymentId = payment.externalPaymentId,
            externalReferenceId = payment.externalReferenceId,
            amount = payment.amount,
            tipAmount = payment.tipAmount,
            taxAmount = payment.taxAmount,
            result = payment.result?.name,
            createdTime = payment.createdTime,
            offline = payment.offline,
            note = payment.note,
            attributes = payment.attributes?.toMap(),
            cardTransaction = payment.cardTransaction?.let {
                CardTransactionInfo(
                    authCode = it.authCode,
                    cardType = it.cardType?.name,
                    currency = it.currency,
                    last4 = it.last4,
                    terminalId = it.extra?.get("terminalId")
                )
            },
            tender = payment.tender?.let {
                TenderInfo(id = it.id, label = it.label, labelKey = it.labelKey)
            },
            transactionInfo = payment.transactionInfo?.let {
                TransactionInfoData(
                    cardTypeLabel = it.cardTypeLabel,
                    entryType = it.entryType?.name,
                    batchNumber = it.batchNumber,
                    receiptNumber = it.receiptNumber,
                    installmentsQuantity = it.installmentsQuantity
                )
            }
        )
    }

    fun toMap(response: PaymentResponse): Map<String, Any?> {
        return mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "payment" to response.payment?.let { toMap(it) }
        )
    }

    fun toMap(payment: PaymentInfo): Map<String, Any?> {
        return mapOf(
            "id" to payment.id,
            "orderId" to payment.orderId,
            "externalPaymentId" to payment.externalPaymentId,
            "externalReferenceId" to payment.externalReferenceId,
            "amount" to payment.amount,
            "tipAmount" to payment.tipAmount,
            "taxAmount" to payment.taxAmount,
            "result" to payment.result,
            "createdTime" to payment.createdTime,
            "offline" to payment.offline,
            "note" to payment.note,
            "attributes" to payment.attributes,
            "cardTransaction" to payment.cardTransaction?.let {
                mapOf(
                    "authCode" to it.authCode,
                    "cardType" to it.cardType,
                    "currency" to it.currency,
                    "last4" to it.last4,
                    "terminalId" to it.terminalId
                )
            },
            "tender" to payment.tender?.let {
                mapOf("id" to it.id, "label" to it.label, "labelKey" to it.labelKey)
            },
            "transactionInfo" to payment.transactionInfo?.let {
                mapOf(
                    "cardTypeLabel" to it.cardTypeLabel,
                    "entryType" to it.entryType,
                    "batchNumber" to it.batchNumber,
                    "receiptNumber" to it.receiptNumber,
                    "installmentsQuantity" to it.installmentsQuantity
                )
            }
        )
    }
}
