package ar.com.orderfast

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.WindowManager
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.services.PaymentService
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
        activity = null
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }
}
