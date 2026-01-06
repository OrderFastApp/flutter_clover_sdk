package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.util.Log
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.models.PaymentResponse
import ar.com.orderfast.mappers.PaymentMapper
import com.clover.connector.sdk.v3.PaymentConnector
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.connector.IPaymentConnectorListener
import com.clover.sdk.v3.payments.TipMode
import com.clover.sdk.v3.remotepay.SaleRequest
import com.clover.sdk.v3.remotepay.SaleResponse
import com.clover.sdk.v3.remotepay.TransactionType

/**
 * Servicio para manejar operaciones de pago con Clover
 */
class PaymentService(
    private val context: Context,
    private val remoteApplicationId: String
) : IPaymentConnectorListener {

    companion object {
        private const val TAG = "PaymentService"
    }

    private var paymentConnector: PaymentConnector? = null
    private var onSaleResponseCallback: ((PaymentResponse) -> Unit)? = null
    private var onDeviceConnectedCallback: (() -> Unit)? = null
    private var onDeviceDisconnectedCallback: (() -> Unit)? = null

    /**
     * Inicializa el PaymentConnector
     */
    fun initialize() {
        try {
            // Obtener la cuenta de Clover usando el método oficial
            val cloverAccount: Account = CloverAccount.getAccount(context)
                ?: throw IllegalStateException("No se pudo obtener la cuenta de Clover")

            // Crear el PaymentConnector con la cuenta y el RAID
            paymentConnector = PaymentConnector(
                context,
                cloverAccount,
                this,
                remoteApplicationId
            )

            Log.d(TAG, "PaymentConnector inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar PaymentConnector", e)
            throw e
        }
    }

    /**
     * Procesa un pago
     */
    fun processPayment(request: PaymentRequest, callback: (PaymentResponse) -> Unit) {
        if (paymentConnector == null) {
            callback(PaymentResponse(
                success = false,
                reason = "NOT_INITIALIZED",
                message = "El PaymentConnector no está inicializado"
            ))
            return
        }

        onSaleResponseCallback = callback

        val saleRequest = SaleRequest().apply {
            externalId = request.externalId
            amount = request.amount
            type = TransactionType.PAYMENT
            disablePrinting = true
            disableReceiptSelection = true
            disableCashback = true
            disableDuplicateChecking = true
            disableRestartTransactionOnFail = true
            tipMode = TipMode.NO_TIP

        }

        try {
            paymentConnector?.sale(saleRequest)
            Log.d(TAG, "Solicitud de pago enviada: ${request.externalId}, monto: ${request.amount}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar pago", e)
            callback(PaymentResponse(
                success = false,
                reason = "EXCEPTION",
                message = e.message
            ))
        }
    }

    /**
     * Desconecta el PaymentConnector
     */
    fun dispose() {
        paymentConnector?.dispose()
        paymentConnector = null
        onSaleResponseCallback = null
        onDeviceConnectedCallback = null
        onDeviceDisconnectedCallback = null
        Log.d(TAG, "PaymentConnector desconectado")
    }

    /**
     * Configura el callback para cuando el dispositivo se conecta
     */
    fun setOnDeviceConnectedCallback(callback: () -> Unit) {
        onDeviceConnectedCallback = callback
    }

    /**
     * Configura el callback para cuando el dispositivo se desconecta
     */
    fun setOnDeviceDisconnectedCallback(callback: () -> Unit) {
        onDeviceDisconnectedCallback = callback
    }

    // IPaymentConnectorListener callbacks

    override fun onDeviceConnected() {
        Log.d(TAG, "Dispositivo conectado")
        onDeviceConnectedCallback?.invoke()
    }

    override fun onDeviceDisconnected() {
        Log.d(TAG, "Dispositivo desconectado")
        onDeviceDisconnectedCallback?.invoke()
    }

    override fun onSaleResponse(response: SaleResponse) {
        Log.d(TAG, "Respuesta de venta recibida: success=${response.success}")
        val paymentResponse = PaymentMapper.toPaymentResponse(response)
        onSaleResponseCallback?.invoke(paymentResponse)
    }

    // Métodos requeridos por IPaymentConnectorListener pero no usados para pagos simples
    override fun onPreAuthResponse(response: com.clover.sdk.v3.remotepay.PreAuthResponse) {}
    override fun onAuthResponse(response: com.clover.sdk.v3.remotepay.AuthResponse) {}
    override fun onTipAdjustAuthResponse(response: com.clover.sdk.v3.remotepay.TipAdjustAuthResponse) {}
    override fun onCapturePreAuthResponse(response: com.clover.sdk.v3.remotepay.CapturePreAuthResponse) {}
    override fun onVerifySignatureRequest(request: com.clover.sdk.v3.remotepay.VerifySignatureRequest) {}
    override fun onConfirmPaymentRequest(request: com.clover.sdk.v3.remotepay.ConfirmPaymentRequest) {}
    override fun onRefundPaymentResponse(response: com.clover.sdk.v3.remotepay.RefundPaymentResponse) {}
    override fun onManualRefundResponse(response: com.clover.sdk.v3.remotepay.ManualRefundResponse) {}
    override fun onVoidPaymentResponse(response: com.clover.sdk.v3.remotepay.VoidPaymentResponse) {}
    override fun onVoidPaymentRefundResponse(response: com.clover.sdk.v3.remotepay.VoidPaymentRefundResponse) {}
    override fun onRetrievePaymentResponse(response: com.clover.sdk.v3.remotepay.RetrievePaymentResponse) {}
    override fun onRetrievePendingPaymentsResponse(response: com.clover.sdk.v3.remotepay.RetrievePendingPaymentsResponse) {}
    override fun onCloseoutResponse(response: com.clover.sdk.v3.remotepay.CloseoutResponse) {}
    override fun onReadCardDataResponse(response: com.clover.sdk.v3.remotepay.ReadCardDataResponse) {}
    override fun onTipAdded(tipAdded: com.clover.sdk.v3.remotepay.TipAdded) {}
    override fun onVaultCardResponse(response: com.clover.sdk.v3.remotepay.VaultCardResponse?) {}
}
