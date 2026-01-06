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
}
