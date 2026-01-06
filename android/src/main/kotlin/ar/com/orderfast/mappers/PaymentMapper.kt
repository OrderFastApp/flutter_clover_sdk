package ar.com.orderfast.mappers

import ar.com.orderfast.models.PaymentInfo
import ar.com.orderfast.models.PaymentResponse
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
            orderId = payment.orderId,
            externalPaymentId = payment.externalPaymentId,
            amount = payment.amount,
            tipAmount = payment.tipAmount,
            taxAmount = payment.taxAmount,
            result = payment.result?.name
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
            "amount" to payment.amount,
            "tipAmount" to payment.tipAmount,
            "taxAmount" to payment.taxAmount,
            "result" to payment.result
        )
    }
}
