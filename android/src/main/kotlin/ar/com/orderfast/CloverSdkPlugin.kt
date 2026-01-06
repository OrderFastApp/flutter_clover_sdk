package ar.com.orderfast

import android.content.Context
import android.util.Log
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.services.PaymentService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/**
 * Plugin principal de Flutter para Clover SDK
 * Maneja la comunicación entre Flutter y el SDK nativo de Clover
 */
class CloverSdkPlugin : FlutterPlugin, MethodCallHandler {

    companion object {
        private const val TAG = "CloverSdkPlugin"
        private const val CHANNEL_NAME = "clover_sdk"
    }

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var paymentService: PaymentService? = null

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

            // Inicializar el servicio
            paymentService?.initialize()

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
        val amount = call.argument<Long>("amount") ?: run {
            result.error("MISSING_AMOUNT", "El monto es requerido", null)
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

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        paymentService?.dispose()
        paymentService = null
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }
}
