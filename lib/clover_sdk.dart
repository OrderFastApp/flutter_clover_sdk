import 'dart:async';
import 'package:flutter/services.dart';
import 'models/models.dart';

/// Plugin para integrar el SDK de Clover en aplicaciones Flutter
/// Simplificado para procesar pagos únicamente
class CloverSdkPlugin {
  static const MethodChannel _channel = MethodChannel('clover_sdk');

  static CloverSdkPlugin? _instance;

  /// Obtiene la instancia singleton del plugin
  static CloverSdkPlugin get instance => _instance ??= CloverSdkPlugin._();

  CloverSdkPlugin._() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  // Callbacks
  Function(Map<String, dynamic>)? onInitialized;
  Function(Map<String, dynamic>)? onDeviceConnected;
  Function(Map<String, dynamic>)? onDeviceDisconnected;
  Function(Map<String, dynamic>)? onSaleResponse;
  Function(Map<String, dynamic>)? onQrPaymentResponse;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onInitialized':
        onInitialized?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onDeviceConnected':
        onDeviceConnected?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onDeviceDisconnected':
        onDeviceDisconnected?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onSaleResponse':
        onSaleResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onQrPaymentResponse':
        onQrPaymentResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
    }
  }

  /// Inicializa el SDK de Clover
  ///
  /// [remoteApplicationId] - ID de la aplicación remota (RAID) - REQUERIDO
  ///
  /// Este ID se obtiene del Clover Developer Dashboard cuando creas tu app
  Future<Map<String, dynamic>> initialize({
    required String remoteApplicationId,
  }) async {
    try {
      final result = await _channel.invokeMethod('initialize', {
        'remoteApplicationId': remoteApplicationId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Procesa un pago (sale)
  ///
  /// [amount] - Monto en centavos (requerido). Ejemplo: $10.00 = 1000 centavos
  /// [externalId] - ID externo único para la transacción (requerido)
  ///
  /// La respuesta real llegará en el callback [onSaleResponse]
  Future<Map<String, dynamic>> sale({
    required int amount,
    required String externalId,
  }) async {
    try {
      final result = await _channel.invokeMethod('sale', {
        'amount': amount,
        'externalId': externalId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Desconecta y libera recursos del SDK
  Future<Map<String, dynamic>> dispose() async {
    try {
      final result = await _channel.invokeMethod('dispose');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Mantiene la pantalla encendida o la libera
  ///
  /// [keepOn] - Si es true, mantiene la pantalla encendida. Si es false, permite que se apague (por defecto: true)
  ///
  /// Útil para aplicaciones POS donde necesitas mantener la pantalla activa durante las transacciones
  Future<Map<String, dynamic>> keepScreenOn({bool keepOn = true}) async {
    try {
      final result = await _channel.invokeMethod('keepScreenOn', {
        'keepOn': keepOn,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Libera el flag que mantiene la pantalla encendida
  ///
  /// Permite que la pantalla se apague normalmente según la configuración del sistema
  Future<Map<String, dynamic>> releaseScreenOn() async {
    try {
      final result = await _channel.invokeMethod('releaseScreenOn');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Activa el modo inmersivo para ocultar la barra de estado y/o navegación
  ///
  /// [hideStatusBar] - Si es true, oculta la barra de estado (por defecto: true)
  /// [hideNavigationBar] - Si es true, oculta la barra de navegación (por defecto: true)
  ///
  /// El modo inmersivo oculta las barras del sistema y las muestra temporalmente
  /// cuando el usuario desliza desde los bordes. Útil para aplicaciones kiosco/POS.
  Future<Map<String, dynamic>> setImmersiveMode({
    bool hideStatusBar = true,
    bool hideNavigationBar = true,
  }) async {
    try {
      final result = await _channel.invokeMethod('setImmersiveMode', {
        'hideStatusBar': hideStatusBar,
        'hideNavigationBar': hideNavigationBar,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Desactiva el modo inmersivo y muestra las barras del sistema
  ///
  /// Restaura la barra de estado y la barra de navegación a su estado normal
  Future<Map<String, dynamic>> exitImmersiveMode() async {
    try {
      final result = await _channel.invokeMethod('exitImmersiveMode');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Activa el modo kiosco (customer mode)
  Future<Map<String, dynamic>> enableKioskMode() async {
    try {
      final result = await _channel.invokeMethod('enableKioskMode');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Desactiva el modo kiosco
  ///
  /// Restaura la funcionalidad normal del sistema y permite salir de la app
  Future<Map<String, dynamic>> disableKioskMode() async {
    try {
      final result = await _channel.invokeMethod('disableKioskMode');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Verifica si el modo kiosco está activo
  ///
  /// Retorna true si el modo kiosco está activo, false en caso contrario
  Future<Map<String, dynamic>> isKioskModeActive() async {
    try {
      final result = await _channel.invokeMethod('isKioskModeActive');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Presenta un código QR para que el cliente escanee y realice el pago
  ///
  /// [amount] - Monto del pago en centavos (ej. 1000 para $10.00)
  /// [externalId] - ID externo único para la transacción
  /// [orderId] - ID de la orden en Clover (opcional)
  ///
  /// Muestra un código QR en la pantalla del dispositivo Clover que el cliente
  /// puede escanear con su app de pago (Mercado Pago, PayPal, etc.) para realizar el pago.
  ///
  /// La respuesta del pago llegará en el callback `onQrPaymentResponse`.
  Future<Map<String, dynamic>> presentQrCode({
    required int amount,
    required String externalId,
    String? orderId,
  }) async {
    try {
      final result = await _channel.invokeMethod('presentQrCode', {
        'amount': amount,
        'externalId': externalId,
        if (orderId != null) 'orderId': orderId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Habilita el screensaver/standby
  ///
  /// [activateOnSleep] - Si es true, el screensaver se activa cuando el dispositivo duerme (por defecto: true)
  /// [dreamComponentPackage] - Nombre del paquete del DreamService personalizado (opcional)
  /// [dreamComponentClass] - Nombre de la clase del DreamService personalizado (opcional)
  ///
  /// El screensaver se mostrará cuando el dispositivo entre en modo standby o cuando
  /// se llame manualmente a `startScreensaver()`.
  Future<Map<String, dynamic>> enableScreensaver({
    bool activateOnSleep = true,
    String? dreamComponentPackage,
    String? dreamComponentClass,
  }) async {
    try {
      final result = await _channel.invokeMethod('enableScreensaver', {
        'activateOnSleep': activateOnSleep,
        if (dreamComponentPackage != null) 'dreamComponentPackage': dreamComponentPackage,
        if (dreamComponentClass != null) 'dreamComponentClass': dreamComponentClass,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Deshabilita el screensaver/standby
  Future<Map<String, dynamic>> disableScreensaver() async {
    try {
      final result = await _channel.invokeMethod('disableScreensaver');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Inicia el screensaver manualmente
  ///
  /// Activa el screensaver inmediatamente sin esperar a que el dispositivo entre en modo standby.
  Future<Map<String, dynamic>> startScreensaver() async {
    try {
      final result = await _channel.invokeMethod('startScreensaver');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Configura el componente DreamService a usar como screensaver
  ///
  /// [packageName] - Nombre del paquete del DreamService
  /// [className] - Nombre completo de la clase del DreamService (incluyendo el paquete)
  ///
  /// Esto permite usar un DreamService personalizado para mostrar contenido personalizado
  /// en el screensaver.
  Future<Map<String, dynamic>> setScreensaverDreamComponent({
    required String packageName,
    required String className,
  }) async {
    try {
      final result = await _channel.invokeMethod('setScreensaverDreamComponent', {
        'packageName': packageName,
        'className': className,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Verifica si el screensaver está habilitado
  ///
  /// Retorna información sobre el estado del screensaver:
  /// - `isEnabled`: Si el screensaver está habilitado
  /// - `activateOnSleep`: Si se activa cuando el dispositivo duerme
  Future<Map<String, dynamic>> isScreensaverEnabled() async {
    try {
      final result = await _channel.invokeMethod('isScreensaverEnabled');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Verifica si el screensaver está soportado en este dispositivo
  ///
  /// Retorna `isSupported`: true si el dispositivo soporta screensaver, false en caso contrario
  Future<Map<String, dynamic>> isScreensaverSupported() async {
    try {
      final result = await _channel.invokeMethod('isScreensaverSupported');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Imprime un ticket (fiscal o no fiscal)
  ///
  /// [nonFiscalTicket] - Datos del ticket no fiscal (opcional). Puede ser un objeto [NonFiscalTicket] o un Map
  /// [fiscalTicket] - Datos del ticket fiscal (opcional). Puede ser un objeto [FiscalTicket] o un Map
  ///
  /// Debe proporcionarse uno de los dos tipos de ticket.
  ///
  /// Ejemplo usando modelos tipados:
  /// ```dart
  /// await cloverSdk.printTicket(
  ///   nonFiscalTicket: NonFiscalTicket(
  ///     orderNumber: 'A73',
  ///     items: [
  ///       TicketItem(
  ///         quantity: 1,
  ///         description: 'Quimera y Haru',
  ///         subtotal: 123.0,
  ///         total: 123.0,
  ///       ),
  ///     ],
  ///     total: 123.0,
  ///     dateTime: '16/01/2026 10:09:49',
  ///   ),
  /// );
  /// ```
  ///
  /// Ejemplo usando Map (también soportado):
  /// ```dart
  /// await cloverSdk.printTicket(
  ///   nonFiscalTicket: {
  ///     'orderNumber': 'A73',
  ///     'items': [
  ///       {
  ///         'quantity': 1,
  ///         'description': 'Quimera y Haru',
  ///         'subtotal': 123.0,
  ///         'total': 123.0,
  ///       },
  ///     ],
  ///     'total': 123.0,
  ///     'dateTime': '16/01/2026 10:09:49',
  ///   },
  /// );
  /// ```
  ///
  /// Ejemplo de ticket fiscal:
  /// ```dart
  /// await cloverSdk.printTicket(
  ///   fiscalTicket: FiscalTicket(
  ///     items: [
  ///       TicketItem(
  ///         quantity: 1,
  ///         description: 'Quimera y Haru',
  ///         subtotal: 123.0,
  ///         total: 123.0,
  ///       ),
  ///     ],
  ///     total: 123.0,
  ///     dateTime: '16/01/2026 10:09:49',
  ///     fiscalInfo: FiscalInfo(
  ///       razonSocial: 'OrderFasts',
  ///       cuit: '20326381285',
  ///       direccion: 'sarmiento 1525',
  ///       localidad: 'San Miguel',
  ///       numeroInscripcionIIBB: '20326381285',
  ///       responsable: 'Monotributo',
  ///       inicioActividades: '6/2025',
  ///       fecha: '16/01/2026',
  ///       numeroT: '00000000344',
  ///       puntoVenta: '0002',
  ///       regimenFiscal: 'Regimen de Transparencia Fiscal al Consumidor Ley 27.743',
  ///       ivaContenido: 21.35,
  ///       otrosImpuestosNacionales: 0.0,
  ///       cae: '86030164034844',
  ///       fechaVencimiento: '26/01/2026',
  ///     ),
  ///   ),
  /// );
  /// ```
  Future<PrintTicketResponse> printTicket({
    dynamic nonFiscalTicket,
    dynamic fiscalTicket,
  }) async {
    try {
      if (nonFiscalTicket == null && fiscalTicket == null) {
        return PrintTicketResponse(
          success: false,
          error: 'Debe proporcionar nonFiscalTicket o fiscalTicket',
        );
      }

      final arguments = <String, dynamic>{};
      if (nonFiscalTicket != null) {
        arguments['nonFiscalTicket'] = nonFiscalTicket is NonFiscalTicket
            ? nonFiscalTicket.toMap()
            : nonFiscalTicket;
      }
      if (fiscalTicket != null) {
        arguments['fiscalTicket'] = fiscalTicket is FiscalTicket
            ? fiscalTicket.toMap()
            : fiscalTicket;
      }

      final result = await _channel.invokeMethod('printTicket', arguments);
      return PrintTicketResponse.fromMap(Map<String, dynamic>.from(result));
    } catch (e) {
      return PrintTicketResponse(
        success: false,
        error: e.toString(),
      );
    }
  }

  /// Imprime un ticket no fiscal de prueba con datos mockeados
  ///
  /// Esta función crea un ticket no fiscal de ejemplo basado en los datos
  /// de las imágenes proporcionadas para pruebas.
  ///
  /// Ejemplo de uso:
  /// ```dart
  /// final result = await cloverSdk.printTestNonFiscalTicket();
  /// if (result.success) {
  ///   print('Ticket impreso correctamente');
  /// } else {
  ///   print('Error: ${result.error}');
  /// }
  /// ```
  Future<PrintTicketResponse> printTestNonFiscalTicket() async {
    final now = DateTime.now();
    final timestamp = _formatDateTime(now);

    final testTicket = NonFiscalTicket(
      orderNumber: 'A73',
      items: [
        TicketItem(
          quantity: 1,
          description: 'Quimera y Haru',
          subtotal: 123.0,
          total: 123.0,
        ),
      ],
      total: 123.0,
      dateTime: timestamp,
      isTakeAway: null, // Puedes cambiar a true o false para probar
      identifier: null, // Puedes agregar un identificador para probar
    );

    return await printTicket(nonFiscalTicket: testTicket);
  }

  /// Imprime un ticket fiscal de prueba con datos mockeados
  ///
  /// Esta función crea un ticket fiscal de ejemplo basado en los datos
  /// de las imágenes proporcionadas para pruebas.
  ///
  /// Ejemplo de uso:
  /// ```dart
  /// final result = await cloverSdk.printTestFiscalTicket();
  /// if (result.success) {
  ///   print('Ticket fiscal impreso correctamente');
  /// } else {
  ///   print('Error: ${result.error}');
  /// }
  /// ```
  Future<PrintTicketResponse> printTestFiscalTicket() async {
    final now = DateTime.now();
    final fecha = _formatDate(now);
    final timestamp = _formatDateTime(now);

    final testTicket = FiscalTicket(
      items: [
        TicketItem(
          quantity: 1,
          description: 'Quimera y Haru',
          subtotal: 123.0,
          total: 123.0,
        ),
      ],
      total: 123.0,
      dateTime: timestamp,
      tipoComprobante: 'Recibo C',
      fiscalInfo: FiscalInfo(
        razonSocial: 'OrderFasts',
        cuit: '20326381285',
        direccion: 'sarmiento 1525',
        localidad: 'San Miguel',
        numeroInscripcionIIBB: '20326381285',
        responsable: 'Monotributo',
        inicioActividades: '6/2025',
        fecha: fecha,
        numeroT: '00000000344',
        puntoVenta: '0002',
        consumidorFinal: true,
        regimenFiscal: 'Regimen de Transparencia Fiscal al Consumidor Ley 27.743',
        ivaContenido: 21.35,
        otrosImpuestosNacionales: 0.0,
        cae: '86030164034844',
        fechaVencimiento: '26/01/2026',
        qrCodeData: 'https://www.sb.orderfast.com/afip?orderId=123',
      ),
    );

    return await printTicket(fiscalTicket: testTicket);
  }

  /// Formatea una fecha como dd/MM/yyyy
  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  /// Formatea una fecha y hora como dd/MM/yyyy HH:mm:ss
  String _formatDateTime(DateTime date) {
    return '${_formatDate(date)} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}:${date.second.toString().padLeft(2, '0')}';
  }
}
