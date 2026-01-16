import 'dart:async';
import 'package:flutter/services.dart';

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
}
