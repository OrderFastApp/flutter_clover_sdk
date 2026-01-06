package ar.com.orderfast

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.models.QrPaymentRequest
import ar.com.orderfast.services.PaymentService
import ar.com.orderfast.services.KioskService
import ar.com.orderfast.services.ImmersiveModeService
import ar.com.orderfast.services.QrPaymentService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/**
 * Plugin principal de Flutter para Clover SDK
 * Maneja la comunicación entre Flutter y el SDK nativo de Clover
 */
class CloverSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    companion object {
        private const val TAG = "CloverSdkPlugin"
        private const val CHANNEL_NAME = "clover_sdk"
    }

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    private var paymentService: PaymentService? = null
    private var qrPaymentService: QrPaymentService? = null
    private var kioskService: KioskService? = null
    private var immersiveModeService: ImmersiveModeService? = null

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
                "sale" -> sale(call, result)
                "dispose" -> dispose(result)
                "keepScreenOn" -> keepScreenOn(call, result)
                "releaseScreenOn" -> releaseScreenOn(result)
                "setImmersiveMode" -> setImmersiveMode(call, result)
                "exitImmersiveMode" -> exitImmersiveMode(result)
                "enableKioskMode" -> enableKioskMode(call, result)
                "disableKioskMode" -> disableKioskMode(call, result)
                "isKioskModeActive" -> isKioskModeActive(result)
                "presentQrCode" -> presentQrCode(call, result)
                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in method ${call.method}", e)
            result.error("ERROR", e.message, null)
        }
    }

    private fun initialize(call: MethodCall, result: MethodChannel.Result) {
        try {
            val remoteApplicationId = call.argument<String>("remoteApplicationId")
                ?: run {
                    result.error("MISSING_RAID", "remoteApplicationId es requerido", null)
                    return
                }

            // Crear el servicio de pagos
            paymentService = PaymentService(context, remoteApplicationId)

            // Configurar callbacks
            paymentService?.setOnDeviceConnectedCallback {
                channel.invokeMethod("onDeviceConnected", mapOf(
                    "success" to true,
                    "message" to "Dispositivo conectado"
                ))
            }

            paymentService?.setOnDeviceDisconnectedCallback {
                channel.invokeMethod("onDeviceDisconnected", mapOf(
                    "success" to false,
                    "message" to "Dispositivo desconectado"
                ))
            }

            // Inicializar el servicio de pagos
            paymentService?.initialize()

            // Inicializar el servicio de pagos QR
            qrPaymentService = QrPaymentService(context)

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

    private fun sale(call: MethodCall, result: MethodChannel.Result) {
        // Flutter envía int como Integer, necesitamos convertirlo a Long
        val amountInt = call.argument<Int>("amount")

        val amount = amountInt?.toLong() ?: run {
            result.error("MISSING_AMOUNT", "El amount es requerido", null)
            return
        }

        val externalId = call.argument<String>("externalId") ?: run {
            result.error("MISSING_EXTERNAL_ID", "El externalId es requerido", null)
            return
        }

        val paymentService = this.paymentService ?: run {
            result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
            return
        }

        val request = PaymentRequest(amount = amount, externalId = externalId)

        paymentService.processPayment(request) { response ->
            val responseMap = ar.com.orderfast.mappers.PaymentMapper.toMap(response)
            channel.invokeMethod("onSaleResponse", responseMap)
        }

        result.success(mapOf(
            "success" to true,
            "message" to "Solicitud de pago enviada"
        ))
    }

    private fun dispose(result: MethodChannel.Result) {
        try {
            paymentService?.dispose()
            paymentService = null
            result.success(mapOf(
                "success" to true,
                "message" to "SDK desconectado correctamente"
            ))
        } catch (e: Exception) {
            result.error("DISPOSE_ERROR", e.message, null)
        }
    }

    private fun keepScreenOn(call: MethodCall, result: MethodChannel.Result) {
        try {
            val keepOn = call.argument<Boolean>("keepOn") ?: true

            activity?.runOnUiThread {
                if (keepOn) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Log.d(TAG, "Pantalla configurada para mantenerse encendida")
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Log.d(TAG, "Flag de mantener pantalla encendida removido")
                }
            }

            result.success(mapOf(
                "success" to true,
                "message" to if (keepOn) "Pantalla configurada para mantenerse encendida" else "Flag removido"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar keep screen on", e)
            result.error("KEEP_SCREEN_ON_ERROR", e.message, null)
        }
    }

    private fun releaseScreenOn(result: MethodChannel.Result) {
        try {
            activity?.runOnUiThread {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "Flag de mantener pantalla encendida removido")
            }

            result.success(mapOf(
                "success" to true,
                "message" to "Flag removido correctamente"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al remover keep screen on", e)
            result.error("RELEASE_SCREEN_ON_ERROR", e.message, null)
        }
    }

    private fun setImmersiveMode(call: MethodCall, result: MethodChannel.Result) {
        try {
            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val hideStatusBar = call.argument<Boolean>("hideStatusBar") ?: true
            val hideNavigationBar = call.argument<Boolean>("hideNavigationBar") ?: true

            // Crear o obtener el servicio de modo inmersivo
            if (immersiveModeService == null) {
                immersiveModeService = ImmersiveModeService()
            }

            immersiveModeService?.enable(activity, hideStatusBar, hideNavigationBar)

            result.success(mapOf(
                "success" to true,
                "message" to "Modo inmersivo activado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar modo inmersivo", e)
            result.error("IMMERSIVE_MODE_ERROR", e.message, null)
        }
    }

    private fun exitImmersiveMode(result: MethodChannel.Result) {
        try {
            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val immersiveModeService = this.immersiveModeService ?: run {
                result.error("NOT_INITIALIZED", "El modo inmersivo no está activo", null)
                return
            }

            immersiveModeService.disable(activity)

            result.success(mapOf(
                "success" to true,
                "message" to "Modo inmersivo desactivado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar modo inmersivo", e)
            result.error("EXIT_IMMERSIVE_MODE_ERROR", e.message, null)
        }
    }

    private fun enableKioskMode(call: MethodCall, result: MethodChannel.Result) {
        try {
            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val unlockCode = call.argument<String>("unlockCode")
            val enableScreenPinning = call.argument<Boolean>("enableScreenPinning") ?: true

            // Crear o obtener el servicio de kiosco
            if (kioskService == null) {
                kioskService = KioskService(context)
            }

            kioskService?.enable(activity, unlockCode, enableScreenPinning)

            result.success(mapOf(
                "success" to true,
                "message" to "Modo kiosco activado. Para desactivar, usa disableKioskMode con el código de desbloqueo."
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar modo kiosco", e)
            result.error("KIOSK_MODE_ERROR", e.message, null)
        }
    }

    private fun disableKioskMode(call: MethodCall, result: MethodChannel.Result) {
        try {
            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val providedCode = call.argument<String>("unlockCode")
            val kioskService = this.kioskService ?: run {
                result.error("NOT_INITIALIZED", "El modo kiosco no está activo", null)
                return
            }

            val success = kioskService.disable(activity, providedCode)
            if (!success) {
                result.error("INVALID_CODE", "Código de desbloqueo incorrecto", null)
                return
            }

            result.success(mapOf(
                "success" to true,
                "message" to "Modo kiosco desactivado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar modo kiosco", e)
            result.error("DISABLE_KIOSK_MODE_ERROR", e.message, null)
        }
    }

    private fun isKioskModeActive(result: MethodChannel.Result) {
        try {
            val kioskService = this.kioskService ?: KioskService(context)
            val isActive = kioskService.isActive()

            result.success(mapOf(
                "success" to true,
                "isActive" to isActive
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado del modo kiosco", e)
            result.error("KIOSK_MODE_CHECK_ERROR", e.message, null)
        }
    }

    private fun presentQrCode(call: MethodCall, result: MethodChannel.Result) {
        try {
            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val amountInt = call.argument<Int>("amount")
            val amount = amountInt?.toLong() ?: run {
                result.error("MISSING_AMOUNT", "El amount es requerido", null)
                return
            }

            val externalId = call.argument<String>("externalId") ?: run {
                result.error("MISSING_EXTERNAL_ID", "El externalId es requerido", null)
                return
            }

            val orderId = call.argument<String>("orderId")

            val qrPaymentService = this.qrPaymentService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            if (!qrPaymentService.isAvailable()) {
                result.error("SERVICE_UNAVAILABLE", "El servicio de pagos QR no está disponible", null)
                return
            }

            val request = QrPaymentRequest(
                amount = amount,
                externalId = externalId,
                orderId = orderId
            )

            qrPaymentService.presentQrCode(activity, request) { response ->
                val responseMap = mapOf(
                    "success" to response.success,
                    "result" to response.result,
                    "reason" to response.reason,
                    "message" to response.message,
                    "qrCodeData" to response.qrCodeData,
                    "payment" to response.payment?.let { payment ->
                        mapOf(
                            "id" to payment.id,
                            "orderId" to payment.orderId,
                            "externalPaymentId" to payment.externalPaymentId,
                            "amount" to payment.amount,
                            "tipAmount" to payment.tipAmount,
                            "taxAmount" to payment.taxAmount,
                            "result" to payment.result
                        )
                    }
                )
                channel.invokeMethod("onQrPaymentResponse", responseMap)
            }

            result.success(mapOf(
                "success" to true,
                "message" to "QR Code presentado. Esperando que el cliente escanee el código."
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al presentar QR Code", e)
            result.error("QR_PAYMENT_ERROR", e.message, null)
        }
    }

    // Método para verificar si se debe bloquear una tecla (llamado desde MainActivity)
    fun shouldBlockKey(keyCode: Int): Boolean {
        return kioskService?.shouldBlockKey(keyCode) ?: false
    }

    // ActivityAware methods
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.d(TAG, "Activity attached")
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
        Log.d(TAG, "Activity detached for config changes")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.d(TAG, "Activity reattached for config changes")
    }

    override fun onDetachedFromActivity() {
        activity = null
        Log.d(TAG, "Activity detached")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        paymentService?.dispose()
        paymentService = null
        qrPaymentService = null
        kioskService?.dispose()
        kioskService = null
        immersiveModeService?.dispose()
        immersiveModeService = null
        activity = null
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }
}
