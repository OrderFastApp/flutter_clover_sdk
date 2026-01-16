package ar.com.orderfast

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import ar.com.orderfast.models.PaymentRequest
import ar.com.orderfast.models.QrPaymentRequest
import ar.com.orderfast.models.NonFiscalTicket
import ar.com.orderfast.models.FiscalTicket
import ar.com.orderfast.models.TicketItem
import ar.com.orderfast.models.TicketSubselection
import ar.com.orderfast.models.FiscalInfo
import ar.com.orderfast.services.PaymentService
import ar.com.orderfast.services.KioskService
import ar.com.orderfast.services.ImmersiveModeService
import ar.com.orderfast.services.QrPaymentService
import ar.com.orderfast.services.ScreensaverService
import ar.com.orderfast.services.PrintService
import android.content.ComponentName
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
    private var screensaverService: ScreensaverService? = null
    private var printService: PrintService? = null

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
                "enableScreensaver" -> enableScreensaver(call, result)
                "disableScreensaver" -> disableScreensaver(result)
                "startScreensaver" -> startScreensaver(result)
                "setScreensaverDreamComponent" -> setScreensaverDreamComponent(call, result)
                "isScreensaverEnabled" -> isScreensaverEnabled(result)
                "isScreensaverSupported" -> isScreensaverSupported(result)
                "printTicket" -> printTicket(call, result)
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

            // Inicializar el servicio de screensaver
            screensaverService = ScreensaverService(context)

            // Inicializar el servicio de impresión
            printService = PrintService(context)

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

            // Crear o obtener el servicio de kiosco
            if (kioskService == null) {
                kioskService = KioskService(context)
            }

            kioskService?.enable(activity)

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

            val kioskService = this.kioskService ?: run {
                result.error("NOT_INITIALIZED", "El modo kiosco no está activo", null)
                return
            }

            val success = kioskService.disable(activity)
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

            val activity = this.activity ?: run {
                result.error("NO_ACTIVITY", "No hay actividad disponible", null)
                return
            }

            val isActive = kioskService.isActive(activity)

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

    private fun enableScreensaver(call: MethodCall, result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            if (!screensaverService.isSupported()) {
                result.error("NOT_SUPPORTED", "El screensaver no está soportado en este dispositivo", null)
                return
            }

            val activateOnSleep = call.argument<Boolean>("activateOnSleep") ?: true
            val dreamComponentPackage = call.argument<String>("dreamComponentPackage")
            val dreamComponentClass = call.argument<String>("dreamComponentClass")

            screensaverService.setEnabled(true)
            screensaverService.setActivateOnSleep(activateOnSleep)

            // Si se proporciona un componente DreamService, configurarlo
            if (dreamComponentPackage != null && dreamComponentClass != null) {
                val componentName = ComponentName(dreamComponentPackage, dreamComponentClass)
                screensaverService.setDreamComponent(componentName)
            }

            result.success(mapOf(
                "success" to true,
                "message" to "Screensaver habilitado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al habilitar screensaver", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun disableScreensaver(result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            screensaverService.setEnabled(false)

            result.success(mapOf(
                "success" to true,
                "message" to "Screensaver deshabilitado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al deshabilitar screensaver", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun startScreensaver(result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            if (!screensaverService.isSupported()) {
                result.error("NOT_SUPPORTED", "El screensaver no está soportado en este dispositivo", null)
                return
            }

            screensaverService.start()

            result.success(mapOf(
                "success" to true,
                "message" to "Screensaver iniciado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar screensaver", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun setScreensaverDreamComponent(call: MethodCall, result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            val packageName = call.argument<String>("packageName") ?: run {
                result.error("MISSING_PACKAGE", "El packageName es requerido", null)
                return
            }

            val className = call.argument<String>("className") ?: run {
                result.error("MISSING_CLASS", "El className es requerido", null)
                return
            }

            val componentName = ComponentName(packageName, className)
            screensaverService.setDreamComponent(componentName)

            result.success(mapOf(
                "success" to true,
                "message" to "Dream component configurado"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar dream component", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun isScreensaverEnabled(result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: run {
                result.error("NOT_INITIALIZED", "El SDK no está inicializado", null)
                return
            }

            val isEnabled = screensaverService.isEnabled()
            val activateOnSleep = screensaverService.isActivateOnSleep()

            result.success(mapOf(
                "success" to true,
                "isEnabled" to isEnabled,
                "activateOnSleep" to activateOnSleep
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado del screensaver", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun isScreensaverSupported(result: MethodChannel.Result) {
        try {
            val screensaverService = this.screensaverService ?: ScreensaverService(context)
            val isSupported = screensaverService.isSupported()

            result.success(mapOf(
                "success" to true,
                "isSupported" to isSupported
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar soporte del screensaver", e)
            result.error("SCREENSAVER_ERROR", e.message, null)
        }
    }

    private fun printTicket(call: MethodCall, result: MethodChannel.Result) {
        try {
            // Crear o obtener el servicio de impresión
            if (printService == null) {
                printService = PrintService(context)
            }

            val nonFiscalData = call.argument<Map<String, Any>>("nonFiscalTicket")
            val fiscalData = call.argument<Map<String, Any>>("fiscalTicket")

            when {
                nonFiscalData != null -> {
                    val ticket = parseNonFiscalTicket(nonFiscalData)
                    printService?.printNonFiscalTicket(ticket) { response ->
                        if (response.success) {
                            result.success(mapOf(
                                "success" to true,
                                "message" to response.message
                            ))
                        } else {
                            result.error("PRINT_ERROR", response.error, null)
                        }
                    }
                }
                fiscalData != null -> {
                    val ticket = parseFiscalTicket(fiscalData)
                    printService?.printFiscalTicket(ticket) { response ->
                        if (response.success) {
                            result.success(mapOf(
                                "success" to true,
                                "message" to response.message
                            ))
                        } else {
                            result.error("PRINT_ERROR", response.error, null)
                        }
                    }
                }
                else -> {
                    result.error("MISSING_TICKET", "Debe proporcionar nonFiscalTicket o fiscalTicket", null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir ticket", e)
            result.error("PRINT_ERROR", e.message, null)
        }
    }

    private fun parseNonFiscalTicket(data: Map<String, Any>): NonFiscalTicket {
        val orderNumber = data["orderNumber"] as? String ?: ""
        val dateTime = data["dateTime"] as? String ?: ""
        val total = (data["total"] as? Number)?.toDouble() ?: 0.0
        val disclaimer = data["disclaimer"] as? String
        val isTakeAway = data["isTakeAway"] as? Boolean
        val identifier = data["identifier"] as? String

        val itemsData = data["items"] as? List<Map<String, Any>> ?: emptyList()
        val items = itemsData.map { itemData ->
            val subselectionsData = itemData["subselections"] as? List<Map<String, Any>>
            val subselections = subselectionsData?.map { subData ->
                TicketSubselection(
                    name = subData["name"] as? String ?: "",
                    quantity = (subData["quantity"] as? Number)?.toInt() ?: 0,
                    price = (subData["price"] as? Number)?.toDouble() ?: 0.0,
                    total = (subData["total"] as? Number)?.toDouble() ?: 0.0
                )
            }

            TicketItem(
                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                description = itemData["description"] as? String ?: "",
                subtotal = (itemData["subtotal"] as? Number)?.toDouble() ?: 0.0,
                total = (itemData["total"] as? Number)?.toDouble() ?: 0.0,
                comment = itemData["comment"] as? String,
                subselections = subselections
            )
        }

        return NonFiscalTicket(
            orderNumber = orderNumber,
            items = items,
            total = total,
            dateTime = dateTime,
            isTakeAway = isTakeAway,
            identifier = identifier,
            disclaimer = disclaimer
        )
    }

    private fun parseFiscalTicket(data: Map<String, Any>): FiscalTicket {
        val orderNumber = data["orderNumber"] as? String
        val dateTime = data["dateTime"] as? String ?: ""
        val total = (data["total"] as? Number)?.toDouble() ?: 0.0
        val tipoComprobante = data["tipoComprobante"] as? String

        val itemsData = data["items"] as? List<Map<String, Any>> ?: emptyList()
        val items = itemsData.map { itemData ->
            val subselectionsData = itemData["subselections"] as? List<Map<String, Any>>
            val subselections = subselectionsData?.map { subData ->
                TicketSubselection(
                    name = subData["name"] as? String ?: "",
                    quantity = (subData["quantity"] as? Number)?.toInt() ?: 0,
                    price = (subData["price"] as? Number)?.toDouble() ?: 0.0,
                    total = (subData["total"] as? Number)?.toDouble() ?: 0.0
                )
            }

            TicketItem(
                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                description = itemData["description"] as? String ?: "",
                subtotal = (itemData["subtotal"] as? Number)?.toDouble() ?: 0.0,
                total = (itemData["total"] as? Number)?.toDouble() ?: 0.0,
                comment = itemData["comment"] as? String,
                subselections = subselections
            )
        }

        val fiscalInfoData = data["fiscalInfo"] as? Map<String, Any>
            ?: throw IllegalArgumentException("fiscalInfo es requerido para tickets fiscales")

        val fiscalInfo = FiscalInfo(
            razonSocial = fiscalInfoData["razonSocial"] as? String ?: "",
            cuit = fiscalInfoData["cuit"] as? String ?: "",
            direccion = fiscalInfoData["direccion"] as? String ?: "",
            localidad = fiscalInfoData["localidad"] as? String ?: "",
            numeroInscripcionIIBB = fiscalInfoData["numeroInscripcionIIBB"] as? String ?: "",
            responsable = fiscalInfoData["responsable"] as? String ?: "",
            inicioActividades = fiscalInfoData["inicioActividades"] as? String ?: "",
            fecha = fiscalInfoData["fecha"] as? String ?: "",
            numeroT = fiscalInfoData["numeroT"] as? String ?: "",
            puntoVenta = fiscalInfoData["puntoVenta"] as? String ?: "",
            consumidorFinal = fiscalInfoData["consumidorFinal"] as? Boolean ?: true,
            regimenFiscal = fiscalInfoData["regimenFiscal"] as? String,
            ivaContenido = (fiscalInfoData["ivaContenido"] as? Number)?.toDouble(),
            otrosImpuestosNacionales = (fiscalInfoData["otrosImpuestosNacionales"] as? Number)?.toDouble(),
            cae = fiscalInfoData["cae"] as? String,
            fechaVencimiento = fiscalInfoData["fechaVencimiento"] as? String,
            qrCodeData = fiscalInfoData["qrCodeData"] as? String
        )

        return FiscalTicket(
            orderNumber = orderNumber,
            items = items,
            total = total,
            dateTime = dateTime,
            fiscalInfo = fiscalInfo,
            tipoComprobante = tipoComprobante
        )
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
        screensaverService = null
        activity = null
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }
}
