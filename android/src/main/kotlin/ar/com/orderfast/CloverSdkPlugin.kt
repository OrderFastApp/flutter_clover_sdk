package ar.com.orderfast

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.clover.connector.sdk.v3.PaymentConnector
import com.clover.sdk.v3.base.Challenge
import com.clover.sdk.v3.connector.IPaymentConnectorListener
import com.clover.sdk.v3.payments.Batch
import com.clover.sdk.v3.base.CardData
import com.clover.sdk.v3.payments.CardTransaction
import com.clover.sdk.v3.payments.Credit
import com.clover.sdk.v3.payments.DataEntryLocation
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.base.PendingPaymentEntry
import com.clover.sdk.v3.payments.Refund
import com.clover.sdk.v3.payments.TipMode
import com.clover.sdk.v3.payments.VaultedCard
import com.clover.sdk.v3.pay.PaymentRequestCardDetails
import com.clover.sdk.v3.remotepay.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class CloverSdkPlugin : FlutterPlugin, MethodCallHandler, IPaymentConnectorListener {

    companion object {
        private const val TAG = "CloverSdkPlugin"
        private const val CHANNEL_NAME = "clover_sdk"
    }

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var paymentConnector: PaymentConnector? = null
    private var account: Account? = null
    private var pendingPaymentForRejection: Payment? = null
    private var pendingChallengeForRejection: Challenge? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        context = binding.applicationContext
        Log.d(TAG, "Plugin attached to engine")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.d(TAG, "Method called: ${call.method}")

        try {
            when (call.method) {
                "initialize" -> initialize(call, result)
                "disconnect" -> disconnect(result)
                "sale" -> sale(call, result)
                "preAuth" -> preAuth(call, result)
                "capturePreAuth" -> capturePreAuth(call, result)
                "tipAdjustAuth" -> tipAdjustAuth(call, result)
                "refundPayment" -> refundPayment(call, result)
                "manualRefund" -> manualRefund(call, result)
                "voidPayment" -> voidPayment(call, result)
                "voidPaymentRefund" -> voidPaymentRefund(call, result)
                "retrievePayment" -> retrievePayment(call, result)
                "retrievePendingPayments" -> retrievePendingPayments(result)
                "confirmPayment" -> confirmPayment(call, result)
                "rejectPayment" -> rejectPayment(call, result)
                "closeout" -> closeout(call, result)
                "readCardData" -> readCardData(call, result)
                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in method ${call.method}", e)
            result.error("ERROR", e.message, null)
        }
    }

    private fun initialize(call: MethodCall, result: MethodChannel.Result) {
        try {
            val accountName = call.argument<String>("accountName")
            val accountType = call.argument<String>("accountType") ?: "com.clover.account"
            val remoteApplicationId = call.argument<String>("remoteApplicationId")

            // Si no se proporciona accountName, usar null para usar la cuenta por defecto
            account = if (accountName != null) {
                Account(accountName, accountType)
            } else {
                null
            }

            // Crear PaymentConnector con el listener
            // Nota: El constructor sin remoteApplicationId está deprecado, pero lo mantenemos para compatibilidad
            paymentConnector = if (remoteApplicationId != null) {
                PaymentConnector(context, account, this, remoteApplicationId)
            } else {
                @Suppress("DEPRECATION")
                PaymentConnector(context, account, this)
            }

            Log.d(TAG, "PaymentConnector creado, conectando...")

            // El PaymentConnector se conecta automáticamente
            // Notificar que se está inicializando
            val response = mapOf(
                "success" to true,
                "message" to "SDK inicializado correctamente"
            )
            channel.invokeMethod("onInitialized", response)
            result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar SDK", e)
            result.error("INIT_ERROR", e.message, null)
        }
    }

    private fun disconnect(result: MethodChannel.Result) {
        try {
            paymentConnector?.dispose()
            paymentConnector = null
            result.success(mapOf("success" to true, "message" to "Desconectado correctamente"))
        } catch (e: Exception) {
            result.error("DISCONNECT_ERROR", e.message, null)
        }
    }

    private fun sale(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Long>("amount") ?: 0L
        val externalId = call.argument<String>("externalId") ?: ""
        val tipAmount = call.argument<Long>("tipAmount") ?: 0L
        val taxAmount = call.argument<Long>("taxAmount") ?: 0L
        val tippableAmount = call.argument<Long>("tippableAmount")
        val orderId = call.argument<String>("orderId")
        val disablePrinting = call.argument<Boolean>("disablePrinting") ?: false
        val disableReceiptSelection = call.argument<Boolean>("disableReceiptSelection") ?: false
        val signatureEntryLocationStr = call.argument<String>("signatureEntryLocation")
        val signatureThreshold = call.argument<Long>("signatureThreshold")
        val cardEntryMethods = call.argument<Int>("cardEntryMethods")
        val vaultedCardId = call.argument<String>("vaultedCard")
        val allowOfflinePayment = call.argument<Boolean>("allowOfflinePayment")
        val approveOfflinePaymentWithoutPrompt =
            call.argument<Boolean>("approveOfflinePaymentWithoutPrompt")
        val tipModeStr = call.argument<String>("tipMode")
        val disableRestartTransactionOnFail =
            call.argument<Boolean>("disableRestartTransactionOnFail")

        val request = SaleRequest().apply {
            this.amount = amount
            this.externalId = externalId
            this.tipAmount = tipAmount
            this.taxAmount = taxAmount
            tippableAmount?.let { this.tippableAmount = it }
            orderId?.let { this.orderId = it }
            this.disablePrinting = disablePrinting
            this.disableReceiptSelection = disableReceiptSelection
            signatureEntryLocationStr?.let {
                try {
                    this.signatureEntryLocation = DataEntryLocation.valueOf(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid signatureEntryLocation: $it", e)
                }
            }
            signatureThreshold?.let { this.signatureThreshold = it }
            cardEntryMethods?.let { this.cardEntryMethods = it }
            vaultedCardId?.let {
                // Crear un VaultedCard con el ID proporcionado
                val vaultedCard = VaultedCard()
                vaultedCard.first6 = it
                this.vaultedCard = vaultedCard
            }
            allowOfflinePayment?.let { this.allowOfflinePayment = it }
            approveOfflinePaymentWithoutPrompt?.let { this.approveOfflinePaymentWithoutPrompt = it }
            tipModeStr?.let {
                try {
                    this.tipMode = TipMode.valueOf(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid tipMode: $it", e)
                }
            }
            disableRestartTransactionOnFail?.let { this.disableRestartTransactionOnFail = it }
        }

        paymentConnector?.sale(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Solicitud de venta enviada"))
    }

    private fun preAuth(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Long>("amount") ?: 0L
        val externalId = call.argument<String>("externalId") ?: ""
        val cardEntryMethods = call.argument<Int>("cardEntryMethods")
        val vaultedCardId = call.argument<String>("vaultedCard")
        val requireNote = call.argument<Boolean>("requireNote")
        val note = call.argument<String>("note")

        val request = PreAuthRequest().apply {
            this.amount = amount
            this.externalId = externalId
            cardEntryMethods?.let { this.cardEntryMethods = it }
            vaultedCardId?.let {
                // Crear un VaultedCard con el ID proporcionado
                val vaultedCard = VaultedCard()
                vaultedCard.first6 = it
                this.vaultedCard = vaultedCard
            }
        }

        paymentConnector?.preAuth(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de pre-autorización enviada"
            )
        )
    }

    private fun capturePreAuth(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Long>("amount") ?: 0L
        val paymentId = call.argument<String>("paymentId") ?: ""
        val tipAmount = call.argument<Long>("tipAmount") ?: 0L

        val request = CapturePreAuthRequest().apply {
            this.amount = amount
            this.paymentId = paymentId
            this.tipAmount = tipAmount
        }

        paymentConnector?.capturePreAuth(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Solicitud de captura enviada"))
    }

    private fun tipAdjustAuth(call: MethodCall, result: MethodChannel.Result) {
        val orderId = call.argument<String>("orderId") ?: ""
        val paymentId = call.argument<String>("paymentId") ?: ""
        val tipAmount = call.argument<Long>("tipAmount") ?: 0L

        val request = TipAdjustAuthRequest().apply {
            this.orderId = orderId
            this.paymentId = paymentId
            this.tipAmount = tipAmount
        }

        paymentConnector?.tipAdjustAuth(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de ajuste de propina enviada"
            )
        )
    }

    private fun refundPayment(call: MethodCall, result: MethodChannel.Result) {
        val orderId = call.argument<String>("orderId") ?: ""
        val paymentId = call.argument<String>("paymentId") ?: ""
        val amount = call.argument<Long>("amount")
        val fullRefund = call.argument<Boolean>("fullRefund") ?: false
        val disablePrinting = call.argument<Boolean>("disablePrinting") ?: false
        val disableReceiptSelection = call.argument<Boolean>("disableReceiptSelection") ?: false

        val request = RefundPaymentRequest().apply {
            this.orderId = orderId
            this.paymentId = paymentId
            amount?.let { this.amount = it }
            this.fullRefund = fullRefund
            this.disablePrinting = disablePrinting
            this.disableReceiptSelection = disableReceiptSelection
        }

        paymentConnector?.refundPayment(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Solicitud de reembolso enviada"))
    }

    private fun manualRefund(call: MethodCall, result: MethodChannel.Result) {
        val amount = call.argument<Long>("amount") ?: 0L
        val externalId = call.argument<String>("externalId") ?: ""
        val cardEntryMethods = call.argument<Int>("cardEntryMethods")
        val disablePrinting = call.argument<Boolean>("disablePrinting") ?: false
        val disableReceiptSelection = call.argument<Boolean>("disableReceiptSelection") ?: false

        val request = ManualRefundRequest().apply {
            this.amount = amount
            this.externalId = externalId
            cardEntryMethods?.let { this.cardEntryMethods = it }
            this.disablePrinting = disablePrinting
            this.disableReceiptSelection = disableReceiptSelection
        }

        paymentConnector?.manualRefund(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de reembolso manual enviada"
            )
        )
    }

    private fun voidPayment(call: MethodCall, result: MethodChannel.Result) {
        val orderId = call.argument<String>("orderId") ?: ""
        val paymentId = call.argument<String>("paymentId") ?: ""
        val voidReason = call.argument<String>("voidReason")
        val disablePrinting = call.argument<Boolean>("disablePrinting") ?: false
        val disableReceiptSelection = call.argument<Boolean>("disableReceiptSelection") ?: false

        val request = VoidPaymentRequest().apply {
            this.orderId = orderId
            this.paymentId = paymentId
            voidReason?.let { this.voidReason = it }
            this.disablePrinting = disablePrinting
            this.disableReceiptSelection = disableReceiptSelection
        }

        paymentConnector?.voidPayment(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Solicitud de anulación enviada"))
    }

    private fun voidPaymentRefund(call: MethodCall, result: MethodChannel.Result) {
        val refundId = call.argument<String>("refundId") ?: ""
        val orderId = call.argument<String>("orderId") ?: ""
        val disablePrinting = call.argument<Boolean>("disablePrinting") ?: false
        val disableReceiptSelection = call.argument<Boolean>("disableReceiptSelection") ?: false

        val request = VoidPaymentRefundRequest().apply {
            this.refundId = refundId
            this.orderId = orderId
            this.disablePrinting = disablePrinting
            this.disableReceiptSelection = disableReceiptSelection
        }

        paymentConnector?.voidPaymentRefund(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de anulación de reembolso enviada"
            )
        )
    }

    private fun retrievePayment(call: MethodCall, result: MethodChannel.Result) {
        val externalPaymentId = call.argument<String>("externalPaymentId") ?: ""

        val request = RetrievePaymentRequest().apply {
            this.externalPaymentId = externalPaymentId
        }

        paymentConnector?.retrievePayment(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de recuperación de pago enviada"
            )
        )
    }

    private fun retrievePendingPayments(result: MethodChannel.Result) {
        paymentConnector?.retrievePendingPayments() ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de pagos pendientes enviada"
            )
        )
    }

    private fun confirmPayment(call: MethodCall, result: MethodChannel.Result) {
        // confirmPayment debe usar el payment y challenges del callback onConfirmPaymentRequest
        val payment = pendingPaymentForRejection
//        val challenge = pendingChallengeForRejection

        if (payment == null) {
            result.error(
                "NO_PENDING_PAYMENT",
                "No hay un pago pendiente para confirmar. Debe llamarse desde onConfirmPaymentRequest",
                null
            )
            return
        }


        paymentConnector?.acceptPayment(payment) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Pago confirmado"))
        // Limpiar los valores pendientes
        pendingPaymentForRejection = null
        pendingChallengeForRejection = null
    }

    private fun rejectPayment(call: MethodCall, result: MethodChannel.Result) {
        // rejectPayment se llama directamente con Payment y Challenge
        // Estos deben venir del callback onConfirmPaymentRequest
        val payment = pendingPaymentForRejection
        val challenge = pendingChallengeForRejection

        if (payment == null || challenge == null) {
            result.error(
                "NO_PENDING_PAYMENT",
                "No hay un pago pendiente para rechazar. Debe llamarse desde onConfirmPaymentRequest",
                null
            )
            return
        }

        try {
            paymentConnector?.rejectPayment(payment, challenge) ?: run {
                result.error("NOT_CONNECTED", "El SDK no está conectado", null)
                return
            }
            result.success(mapOf("success" to true, "message" to "Pago rechazado"))
            // Limpiar los valores pendientes
            pendingPaymentForRejection = null
            pendingChallengeForRejection = null
        } catch (e: Exception) {
            result.error("REJECT_ERROR", e.message, null)
        }
    }

    private fun closeout(call: MethodCall, result: MethodChannel.Result) {
        val allowOpenTabs = call.argument<Boolean>("allowOpenTabs") ?: false
        val batchId = call.argument<String>("batchId")

        val request = CloseoutRequest().apply {
            this.allowOpenTabs = allowOpenTabs
            batchId?.let { this.batchId = it }
        }

        paymentConnector?.closeout(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(mapOf("success" to true, "message" to "Solicitud de cierre enviada"))
    }

    private fun readCardData(call: MethodCall, result: MethodChannel.Result) {
        val cardEntryMethods = call.argument<Int>("cardEntryMethods")

        val request = ReadCardDataRequest().apply {
            cardEntryMethods?.let { this.cardEntryMethods = it }
        }

        paymentConnector?.readCardData(request) ?: run {
            result.error("NOT_CONNECTED", "El SDK no está conectado", null)
            return
        }

        result.success(
            mapOf(
                "success" to true,
                "message" to "Solicitud de lectura de tarjeta enviada"
            )
        )
    }

    // IPaymentConnectorListener callbacks
    override fun onSaleResponse(response: SaleResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "payment" to response.payment?.let { convertPaymentToMap(it) },
            "externalPaymentId" to response.payment.externalPaymentId
        )
        channel.invokeMethod("onSaleResponse", result)
    }

    override fun onDeviceDisconnected() {
        Log.d(TAG, "Dispositivo desconectado")
        val result = mapOf(
            "success" to false,
            "message" to "Dispositivo desconectado"
        )
        channel.invokeMethod("onDeviceDisconnected", result)
    }

    override fun onDeviceConnected() {
        Log.d(TAG, "Dispositivo conectado")
        val result = mapOf(
            "success" to true,
            "message" to "Dispositivo conectado"
        )
        channel.invokeMethod("onDeviceConnected", result)
    }

    override fun onPreAuthResponse(response: PreAuthResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "payment" to response.payment?.let { convertPaymentToMap(it) },
            "externalPaymentId" to response.payment.externalPaymentId
        )
        channel.invokeMethod("onPreAuthResponse", result)
    }

    override fun onAuthResponse(response: AuthResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "payment" to response.payment?.let { convertPaymentToMap(it) },
            "externalPaymentId" to response.payment.externalPaymentId
        )
        channel.invokeMethod("onAuthResponse", result)
    }

    override fun onTipAdjustAuthResponse(response: TipAdjustAuthResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "paymentId" to response.paymentId,
            "tipAmount" to response.tipAmount
        )
        channel.invokeMethod("onTipAdjustAuthResponse", result)
    }

    override fun onCapturePreAuthResponse(response: CapturePreAuthResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "paymentId" to response.paymentId,
            "amount" to response.amount,
            "tipAmount" to response.tipAmount
        )
        channel.invokeMethod("onCapturePreAuthResponse", result)
    }

    override fun onVerifySignatureRequest(request: VerifySignatureRequest) {
        val result = mapOf(
            "payment" to request.payment?.let { convertPaymentToMap(it) },
            "signature" to request.signature
        )
        channel.invokeMethod("onVerifySignatureRequest", result)
    }

    override fun onConfirmPaymentRequest(request: ConfirmPaymentRequest) {
        // Almacenar el payment y challenge para poder usarlos en rejectPayment
        pendingPaymentForRejection = request.payment
        pendingChallengeForRejection = request.challenges?.firstOrNull()

        val result = mapOf(
            "payment" to request.payment?.let { convertPaymentToMap(it) },
            "challenges" to request.challenges?.map { convertChallengeToMap(it) }
        )
        channel.invokeMethod("onConfirmPaymentRequest", result)
    }

    override fun onRefundPaymentResponse(response: RefundPaymentResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "refund" to response.refund?.let { convertRefundToMap(it) },
            "orderId" to response.orderId,
            "paymentId" to response.paymentId
        )
        channel.invokeMethod("onRefundPaymentResponse", result)
    }

    override fun onManualRefundResponse(response: ManualRefundResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "credit" to response.credit?.let { convertCreditToMap(it) },
            "externalPaymentId" to response.credit.externalReferenceId
        )
        channel.invokeMethod("onManualRefundResponse", result)
    }

    override fun onVoidPaymentResponse(response: VoidPaymentResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "paymentId" to response.paymentId,
            "voidReason" to response.reason
        )
        channel.invokeMethod("onVoidPaymentResponse", result)
    }

    override fun onVaultCardResponse(response: VaultCardResponse?) {
        if (response == null) {
            Log.w(TAG, "onVaultCardResponse recibido con response null")
            return
        }

        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "card" to response.card?.let { convertVaultedCardToMap(it) },
            "cardDetails" to response.cardDetails?.let { convertCardDetailsToMap(it) }
        )
        channel.invokeMethod("onVaultCardResponse", result)
    }

    override fun onVoidPaymentRefundResponse(response: VoidPaymentRefundResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "refundId" to response.refundId,
            "paymentId" to response.paymentId,
            "orderId" to response.orderId
        )
        channel.invokeMethod("onVoidPaymentRefundResponse", result)
    }

    override fun onRetrievePaymentResponse(response: RetrievePaymentResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "queryStatus" to response.queryStatus,
            "externalPaymentId" to response.externalPaymentId,
            "payment" to response.payment?.let { convertPaymentToMap(it) }
        )
        channel.invokeMethod("onRetrievePaymentResponse", result)
    }

    override fun onRetrievePendingPaymentsResponse(response: RetrievePendingPaymentsResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "pendingPaymentEntries" to response.pendingPaymentEntries?.map {
                convertPendingPaymentToMap(
                    it
                )
            }
        )
        channel.invokeMethod("onRetrievePendingPaymentsResponse", result)
    }

    override fun onCloseoutResponse(response: CloseoutResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "batch" to response.batch?.let { convertBatchToMap(it) }
        )
        channel.invokeMethod("onCloseoutResponse", result)
    }

    override fun onReadCardDataResponse(response: ReadCardDataResponse) {
        val result = mapOf(
            "success" to response.success,
            "result" to response.result,
            "reason" to response.reason,
            "message" to response.message,
            "cardData" to response.cardData?.let { convertCardDataToMap(it) }
        )
        channel.invokeMethod("onReadCardDataResponse", result)
    }

    override fun onTipAdded(tipAdded: TipAdded) {
        val result = mapOf(
            "tipAmount" to tipAdded.tipAmount,
        )
        channel.invokeMethod("onTipAdded", result)
    }

    // Helper methods to convert objects to maps
    private fun convertPaymentToMap(payment: Payment): Map<String, Any?> {
        return mapOf(
            "id" to payment.id,
            "orderId" to payment.order.id,
            "externalPaymentId" to payment.externalPaymentId,
            "amount" to payment.amount,
            "tipAmount" to payment.tipAmount,
            "taxAmount" to payment.taxAmount,
            "cashbackAmount" to payment.cashbackAmount,
            "result" to payment.result,
            "cardTransaction" to payment.cardTransaction?.let { convertCardTransactionToMap(it) }
        )
    }

    private fun convertCardTransactionToMap(transaction: CardTransaction): Map<String, Any?> {
        return mapOf(
            "authCode" to transaction.authCode,
            "cardType" to transaction.cardType,
            "entryType" to transaction.entryType,
            "first6" to transaction.first6,
            "last4" to transaction.last4,
            "type" to transaction.type
        )
    }

    private fun convertRefundToMap(refund: Refund): Map<String, Any?> {
        return mapOf(
            "id" to refund.id,
            "orderId" to refund.externalReferenceId,
            "paymentId" to refund.payment.id,
            "amount" to refund.amount,
            "result" to refund.reason
        )
    }

    private fun convertCreditToMap(credit: Credit): Map<String, Any?> {
        return try {
            mapOf(
                "id" to (credit.id ?: ""),
                "amount" to (credit.amount ?: 0L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Credit to map", e)
            emptyMap()
        }
    }

    private fun convertChallengeToMap(challenge: Challenge): Map<String, Any?> {
        return mapOf(
            "type" to challenge.type,
            "message" to challenge.message
        )
    }

    private fun convertPendingPaymentToMap(entry: PendingPaymentEntry): Map<String, Any?> {
        return mapOf(
            "paymentId" to entry.paymentId,
            "orderId" to entry.externalPaymentId,
            "amount" to entry.amount
        )
    }

    private fun convertBatchToMap(batch: Batch): Map<String, Any?> {
        return mapOf(
            "id" to batch.id,
            "state" to batch.state
        )
    }

    private fun convertCardDataToMap(cardData: CardData): Map<String, Any?> {
        return try {
            mapOf(
                "first6" to (cardData.first6 ?: ""),
                "last4" to (cardData.last4 ?: ""),
                "cardType" to "",
                "entryType" to ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting CardData to map", e)
            emptyMap()
        }
    }

    private fun convertVaultedCardToMap(vaultedCard: VaultedCard): Map<String, Any?> {
        return try {
            mapOf(
                "id" to (""),
                "first6" to (vaultedCard.first6 ?: ""),
                "last4" to (vaultedCard.last4 ?: ""),
                "cardType" to (""),
                "expirationDate" to (vaultedCard.expirationDate ?: ""),
                "token" to (vaultedCard.token ?: "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting VaultedCard to map", e)
            emptyMap()
        }
    }

    private fun convertCardDetailsToMap(cardDetails: PaymentRequestCardDetails): Map<String, Any?> {
        return try {
            mapOf(
                "cardNumber" to (""),
                "expirationDate" to (cardDetails.exp ?: ""),
                "cvv" to (cardDetails.cvv ?: ""),
                "zipCode" to (cardDetails.zip ?: ""),
                "cardType" to (cardDetails.cardType?.name ?: ""),
                "first6" to (""),
                "last4" to (cardDetails.last4 ?: "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting PaymentRequestCardDetails to map", e)
            emptyMap()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        paymentConnector?.dispose()
        paymentConnector = null
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }
}