import 'dart:async';
import 'package:flutter/services.dart';

/// Plugin para integrar el SDK de Clover en aplicaciones Flutter
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
  Function(Map<String, dynamic>)? onServiceDisconnected;
  Function(Map<String, dynamic>)? onDeviceConnected;
  Function(Map<String, dynamic>)? onDeviceDisconnected;
  Function(Map<String, dynamic>)? onSaleResponse;
  Function(Map<String, dynamic>)? onPreAuthResponse;
  Function(Map<String, dynamic>)? onAuthResponse;
  Function(Map<String, dynamic>)? onTipAdjustAuthResponse;
  Function(Map<String, dynamic>)? onCapturePreAuthResponse;
  Function(Map<String, dynamic>)? onVerifySignatureRequest;
  Function(Map<String, dynamic>)? onConfirmPaymentRequest;
  Function(Map<String, dynamic>)? onRefundPaymentResponse;
  Function(Map<String, dynamic>)? onManualRefundResponse;
  Function(Map<String, dynamic>)? onVoidPaymentResponse;
  Function(Map<String, dynamic>)? onVoidPaymentRefundResponse;
  Function(Map<String, dynamic>)? onRetrievePaymentResponse;
  Function(Map<String, dynamic>)? onRetrievePendingPaymentsResponse;
  Function(Map<String, dynamic>)? onCloseoutResponse;
  Function(Map<String, dynamic>)? onReadCardDataResponse;
  Function(Map<String, dynamic>)? onTipAdded;
  Function(Map<String, dynamic>)? onVaultCardResponse;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onInitialized':
        onInitialized?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onServiceDisconnected':
        onServiceDisconnected?.call(Map<String, dynamic>.from(call.arguments));
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
      case 'onPreAuthResponse':
        onPreAuthResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onAuthResponse':
        onAuthResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onTipAdjustAuthResponse':
        onTipAdjustAuthResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onCapturePreAuthResponse':
        onCapturePreAuthResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onVerifySignatureRequest':
        onVerifySignatureRequest?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onConfirmPaymentRequest':
        onConfirmPaymentRequest?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onRefundPaymentResponse':
        onRefundPaymentResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onManualRefundResponse':
        onManualRefundResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onVoidPaymentResponse':
        onVoidPaymentResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onVoidPaymentRefundResponse':
        onVoidPaymentRefundResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onRetrievePaymentResponse':
        onRetrievePaymentResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onRetrievePendingPaymentsResponse':
        onRetrievePendingPaymentsResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onCloseoutResponse':
        onCloseoutResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onReadCardDataResponse':
        onReadCardDataResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onTipAdded':
        onTipAdded?.call(Map<String, dynamic>.from(call.arguments));
        break;
      case 'onVaultCardResponse':
        onVaultCardResponse?.call(Map<String, dynamic>.from(call.arguments));
        break;
    }
  }

  /// Inicializa el SDK de Clover
  ///
  /// [accountName] - Nombre de la cuenta (opcional, si no se proporciona usa la cuenta por defecto)
  /// [accountType] - Tipo de cuenta (por defecto: "com.clover.account")
  /// [remoteApplicationId] - ID de la aplicación remota (opcional, requerido para algunas integraciones)
  Future<Map<String, dynamic>> initialize({
    String? accountName,
    String accountType = 'com.clover.account',
    String? remoteApplicationId,
  }) async {
    try {
      final result = await _channel.invokeMethod('initialize', {
        if (accountName != null) 'accountName': accountName,
        'accountType': accountType,
        if (remoteApplicationId != null) 'remoteApplicationId': remoteApplicationId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Desconecta el SDK de Clover
  Future<Map<String, dynamic>> disconnect() async {
    try {
      final result = await _channel.invokeMethod('disconnect');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Realiza una venta (pago)
  ///
  /// [amount] - Monto en centavos (requerido)
  /// [externalId] - ID externo único para la transacción (requerido)
  /// [tipAmount] - Monto de la propina en centavos (opcional)
  /// [taxAmount] - Monto del impuesto en centavos (opcional)
  /// [tippableAmount] - Monto sobre el cual se puede agregar propina (opcional)
  /// [orderId] - ID de la orden (opcional)
  /// [disablePrinting] - Deshabilitar impresión (opcional)
  /// [disableReceiptSelection] - Deshabilitar selección de recibo (opcional)
  /// [signatureEntryLocation] - Ubicación de entrada de firma (opcional)
  /// [signatureThreshold] - Umbral para requerir firma (opcional)
  /// [cardEntryMethods] - Métodos de entrada de tarjeta (opcional)
  /// [vaultedCard] - Tarjeta almacenada (opcional)
  /// [allowOfflinePayment] - Permitir pago offline (opcional)
  /// [approveOfflinePaymentWithoutPrompt] - Aprobar pago offline sin prompt (opcional)
  /// [requireNote] - Requerir nota (opcional)
  /// [note] - Nota (opcional)
  /// [tipMode] - Modo de propina (opcional)
  /// [disableRestartTransactionOnFail] - Deshabilitar reinicio de transacción en fallo (opcional)
  Future<Map<String, dynamic>> sale({
    required int amount,
    required String externalId,
    int tipAmount = 0,
    int taxAmount = 0,
    int? tippableAmount,
    String? orderId,
    bool disablePrinting = false,
    bool disableReceiptSelection = false,
    String? signatureEntryLocation,
    int? signatureThreshold,
    int? cardEntryMethods,
    int? vaultedCard,
    bool? allowOfflinePayment,
    bool? approveOfflinePaymentWithoutPrompt,
    bool? requireNote,
    String? note,
    String? tipMode,
    bool? disableRestartTransactionOnFail,
  }) async {
    try {
      final result = await _channel.invokeMethod('sale', {
        'amount': amount,
        'externalId': externalId,
        'tipAmount': tipAmount,
        'taxAmount': taxAmount,
        if (tippableAmount != null) 'tippableAmount': tippableAmount,
        if (orderId != null) 'orderId': orderId,
        'disablePrinting': disablePrinting,
        'disableReceiptSelection': disableReceiptSelection,
        if (signatureEntryLocation != null) 'signatureEntryLocation': signatureEntryLocation,
        if (signatureThreshold != null) 'signatureThreshold': signatureThreshold,
        if (cardEntryMethods != null) 'cardEntryMethods': cardEntryMethods,
        if (vaultedCard != null) 'vaultedCard': vaultedCard,
        if (allowOfflinePayment != null) 'allowOfflinePayment': allowOfflinePayment,
        if (approveOfflinePaymentWithoutPrompt != null) 'approveOfflinePaymentWithoutPrompt': approveOfflinePaymentWithoutPrompt,
        if (requireNote != null) 'requireNote': requireNote,
        if (note != null) 'note': note,
        if (tipMode != null) 'tipMode': tipMode,
        if (disableRestartTransactionOnFail != null) 'disableRestartTransactionOnFail': disableRestartTransactionOnFail,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Realiza una pre-autorización
  ///
  /// [amount] - Monto en centavos (requerido)
  /// [externalId] - ID externo único (requerido)
  /// [cardEntryMethods] - Métodos de entrada de tarjeta (opcional)
  /// [vaultedCard] - Tarjeta almacenada (opcional)
  /// [requireNote] - Requerir nota (opcional)
  /// [note] - Nota (opcional)
  Future<Map<String, dynamic>> preAuth({
    required int amount,
    required String externalId,
    int? cardEntryMethods,
    int? vaultedCard,
    bool? requireNote,
    String? note,
  }) async {
    try {
      final result = await _channel.invokeMethod('preAuth', {
        'amount': amount,
        'externalId': externalId,
        if (cardEntryMethods != null) 'cardEntryMethods': cardEntryMethods,
        if (vaultedCard != null) 'vaultedCard': vaultedCard,
        if (requireNote != null) 'requireNote': requireNote,
        if (note != null) 'note': note,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Captura una pre-autorización
  ///
  /// [amount] - Monto a capturar en centavos (requerido)
  /// [paymentId] - ID del pago pre-autorizado (requerido)
  /// [tipAmount] - Monto de la propina en centavos (opcional)
  Future<Map<String, dynamic>> capturePreAuth({
    required int amount,
    required String paymentId,
    int tipAmount = 0,
  }) async {
    try {
      final result = await _channel.invokeMethod('capturePreAuth', {
        'amount': amount,
        'paymentId': paymentId,
        'tipAmount': tipAmount,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Ajusta la propina de una autorización
  ///
  /// [orderId] - ID de la orden (requerido)
  /// [paymentId] - ID del pago (requerido)
  /// [tipAmount] - Nuevo monto de propina en centavos (requerido)
  Future<Map<String, dynamic>> tipAdjustAuth({
    required String orderId,
    required String paymentId,
    required int tipAmount,
  }) async {
    try {
      final result = await _channel.invokeMethod('tipAdjustAuth', {
        'orderId': orderId,
        'paymentId': paymentId,
        'tipAmount': tipAmount,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Reembolsa un pago
  ///
  /// [orderId] - ID de la orden (requerido)
  /// [paymentId] - ID del pago (requerido)
  /// [amount] - Monto a reembolsar en centavos (opcional, si no se especifica es reembolso completo)
  /// [fullRefund] - Si es reembolso completo (opcional)
  /// [disablePrinting] - Deshabilitar impresión (opcional)
  /// [disableReceiptSelection] - Deshabilitar selección de recibo (opcional)
  Future<Map<String, dynamic>> refundPayment({
    required String orderId,
    required String paymentId,
    int? amount,
    bool fullRefund = false,
    bool disablePrinting = false,
    bool disableReceiptSelection = false,
  }) async {
    try {
      final result = await _channel.invokeMethod('refundPayment', {
        'orderId': orderId,
        'paymentId': paymentId,
        if (amount != null) 'amount': amount,
        'fullRefund': fullRefund,
        'disablePrinting': disablePrinting,
        'disableReceiptSelection': disableReceiptSelection,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Realiza un reembolso manual (sin pago previo)
  ///
  /// [amount] - Monto en centavos (requerido)
  /// [externalId] - ID externo único (requerido)
  /// [cardEntryMethods] - Métodos de entrada de tarjeta (opcional)
  /// [disablePrinting] - Deshabilitar impresión (opcional)
  /// [disableReceiptSelection] - Deshabilitar selección de recibo (opcional)
  /// [requireNote] - Requerir nota (opcional)
  /// [note] - Nota (opcional)
  Future<Map<String, dynamic>> manualRefund({
    required int amount,
    required String externalId,
    int? cardEntryMethods,
    bool disablePrinting = false,
    bool disableReceiptSelection = false,
    bool? requireNote,
    String? note,
  }) async {
    try {
      final result = await _channel.invokeMethod('manualRefund', {
        'amount': amount,
        'externalId': externalId,
        if (cardEntryMethods != null) 'cardEntryMethods': cardEntryMethods,
        'disablePrinting': disablePrinting,
        'disableReceiptSelection': disableReceiptSelection,
        if (requireNote != null) 'requireNote': requireNote,
        if (note != null) 'note': note,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Anula un pago
  ///
  /// [orderId] - ID de la orden (requerido)
  /// [paymentId] - ID del pago (requerido)
  /// [voidReason] - Razón de la anulación (opcional)
  /// [disablePrinting] - Deshabilitar impresión (opcional)
  /// [disableReceiptSelection] - Deshabilitar selección de recibo (opcional)
  Future<Map<String, dynamic>> voidPayment({
    required String orderId,
    required String paymentId,
    String? voidReason,
    bool disablePrinting = false,
    bool disableReceiptSelection = false,
  }) async {
    try {
      final result = await _channel.invokeMethod('voidPayment', {
        'orderId': orderId,
        'paymentId': paymentId,
        if (voidReason != null) 'voidReason': voidReason,
        'disablePrinting': disablePrinting,
        'disableReceiptSelection': disableReceiptSelection,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Anula un reembolso
  ///
  /// [refundId] - ID del reembolso (requerido)
  /// [orderId] - ID de la orden (requerido)
  /// [paymentId] - ID del pago (requerido)
  /// [disablePrinting] - Deshabilitar impresión (opcional)
  /// [disableReceiptSelection] - Deshabilitar selección de recibo (opcional)
  Future<Map<String, dynamic>> voidPaymentRefund({
    required String refundId,
    required String orderId,
    required String paymentId,
    bool disablePrinting = false,
    bool disableReceiptSelection = false,
  }) async {
    try {
      final result = await _channel.invokeMethod('voidPaymentRefund', {
        'refundId': refundId,
        'orderId': orderId,
        'paymentId': paymentId,
        'disablePrinting': disablePrinting,
        'disableReceiptSelection': disableReceiptSelection,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Recupera información de un pago
  ///
  /// [externalPaymentId] - ID externo del pago (requerido)
  Future<Map<String, dynamic>> retrievePayment({
    required String externalPaymentId,
  }) async {
    try {
      final result = await _channel.invokeMethod('retrievePayment', {
        'externalPaymentId': externalPaymentId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Recupera todos los pagos pendientes
  Future<Map<String, dynamic>> retrievePendingPayments() async {
    try {
      final result = await _channel.invokeMethod('retrievePendingPayments');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Confirma un pago (usado en respuesta a onConfirmPaymentRequest)
  ///
  /// Nota: Este método requiere que el pago y los challenges vengan del callback onConfirmPaymentRequest
  Future<Map<String, dynamic>> confirmPayment({
    required Map<String, dynamic> payment,
    List<Map<String, dynamic>>? challenges,
  }) async {
    try {
      final result = await _channel.invokeMethod('confirmPayment', {
        'payment': payment,
        if (challenges != null) 'challenges': challenges,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Rechaza un pago (usado en respuesta a onConfirmPaymentRequest)
  ///
  /// Nota: Este método requiere que el pago y el challenge vengan del callback onConfirmPaymentRequest
  Future<Map<String, dynamic>> rejectPayment({
    required Map<String, dynamic> payment,
    required String reason,
  }) async {
    try {
      final result = await _channel.invokeMethod('rejectPayment', {
        'payment': payment,
        'reason': reason,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Realiza un cierre de lote
  ///
  /// [allowOpenTabs] - Permitir pestañas abiertas (opcional)
  /// [batchId] - ID del lote (opcional)
  Future<Map<String, dynamic>> closeout({
    bool allowOpenTabs = false,
    String? batchId,
  }) async {
    try {
      final result = await _channel.invokeMethod('closeout', {
        'allowOpenTabs': allowOpenTabs,
        if (batchId != null) 'batchId': batchId,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Lee datos de una tarjeta
  ///
  /// [cardEntryMethods] - Métodos de entrada de tarjeta (opcional)
  Future<Map<String, dynamic>> readCardData({
    int? cardEntryMethods,
  }) async {
    try {
      final result = await _channel.invokeMethod('readCardData', {
        if (cardEntryMethods != null) 'cardEntryMethods': cardEntryMethods,
      });
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }
}
