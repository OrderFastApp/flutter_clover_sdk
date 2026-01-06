package ar.com.orderfast

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    private var immersiveModeActive = false

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

    private fun setImmersiveMode(call: MethodCall, result: MethodChannel.Result) {
        try {
            val hideStatusBar = call.argument<Boolean>("hideStatusBar") ?: true
            val hideNavigationBar = call.argument<Boolean>("hideNavigationBar") ?: true

            activity?.runOnUiThread {
                val window = activity?.window ?: return@runOnUiThread
                val decorView = window.decorView

                // Habilitar edge-to-edge
                WindowCompat.setDecorFitsSystemWindows(window, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    val controller = window.insetsController
                    if (controller != null) {
                        var typesToHide = 0

                        if (hideStatusBar) {
                            typesToHide = typesToHide or WindowInsetsCompat.Type.statusBars()
                        }

                        if (hideNavigationBar) {
                            typesToHide = typesToHide or WindowInsetsCompat.Type.navigationBars()
                        }

                        if (typesToHide != 0) {
                            controller.hide(typesToHide)
                            // Mantener el modo inmersivo sticky
                            controller.systemBarsBehavior =
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }
                } else {
                    // Android 10 y anteriores
                    @Suppress("DEPRECATION")
                    var flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

                    if (hideStatusBar) {
                        @Suppress("DEPRECATION")
                        flags = flags or View.SYSTEM_UI_FLAG_FULLSCREEN
                    }

                    if (hideNavigationBar) {
                        @Suppress("DEPRECATION")
                        flags = flags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    }

                    // Modo inmersivo sticky
                    @Suppress("DEPRECATION")
                    flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                    @Suppress("DEPRECATION")
                    decorView.systemUiVisibility = flags
                }

                immersiveModeActive = true

                Log.d(TAG, "Modo inmersivo activado: statusBar=$hideStatusBar, navigationBar=$hideNavigationBar")
            }

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
            activity?.runOnUiThread {
                val window = activity?.window ?: return@runOnUiThread
                val decorView = window.decorView

                immersiveModeActive = false

                // Deshabilitar edge-to-edge
                WindowCompat.setDecorFitsSystemWindows(window, true)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+)
                    val controller = window.insetsController
                    if (controller != null) {
                        controller.show(WindowInsetsCompat.Type.statusBars())
                        controller.show(WindowInsetsCompat.Type.navigationBars())
                    }
                } else {
                    // Android 10 y anteriores
                    @Suppress("DEPRECATION")
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }

                Log.d(TAG, "Modo inmersivo desactivado")
            }

            result.success(mapOf(
                "success" to true,
                "message" to "Modo inmersivo desactivado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar modo inmersivo", e)
            result.error("EXIT_IMMERSIVE_MODE_ERROR", e.message, null)
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
