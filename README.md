# Clover SDK para Flutter

[![Version](https://img.shields.io/badge/version-0.3.7-blue.svg)](https://pub.dev/packages/clover_sdk)
[![Flutter](https://img.shields.io/badge/flutter-%3E%3D3.3.0-blue.svg)](https://flutter.dev)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Plugin de Flutter para integrar el SDK de Clover en aplicaciones Android, permitiendo procesar pagos, reembolsos, pre-autorizaciones y más funcionalidades del ecosistema Clover.

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso Básico](#-uso-básico)
- [Ejemplos](#-ejemplos)
- [API Completa](#-api-completa)
- [Callbacks y Eventos](#-callbacks-y-eventos)
- [Manejo de Errores](#-manejo-de-errores)
- [Troubleshooting](#-troubleshooting)

## ✨ Características

- ✅ **Pagos (Sales)**: Procesar pagos con tarjeta
- ✅ **Pre-autorizaciones**: Autorizar pagos antes de capturarlos
- ✅ **Reembolsos**: Reembolsos parciales y completos
- ✅ **Reembolsos Manuales**: Reembolsos sin pago previo
- ✅ **Anulaciones**: Anular pagos y reembolsos
- ✅ **Consultas**: Recuperar información de pagos y pagos pendientes
- ✅ **Cierre de Lote**: Realizar cierres de lote
- ✅ **Lectura de Tarjetas**: Leer datos de tarjetas
- ✅ **Callbacks Completos**: Sistema completo de eventos y callbacks

## 📦 Instalación

Agrega la dependencia a tu archivo `pubspec.yaml`:

```yaml
dependencies:
  clover_sdk:
    path: ../clover_sdk  # Si es local
    # O desde pub.dev cuando esté publicado
    # clover_sdk: ^0.3.7
```

Luego ejecuta:

```bash
flutter pub get
```

## ⚙️ Configuración

### Android

El plugin requiere permisos específicos de Clover. Estos ya están incluidos en el plugin, pero asegúrate de que tu aplicación tenga los permisos necesarios:

```xml
<!-- Permisos necesarios para el SDK de Clover -->
<uses-permission android:name="com.clover.permission.LAUNCH_PAYMENTS" />
<uses-permission android:name="com.clover.permission.READ_MERCHANT" />
```

### Inicialización

El SDK se inicializa automáticamente cuando llamas al método `initialize()`. No requiere configuración adicional en el código.

## 🚀 Uso Básico

### 1. Importar el plugin

```dart
import 'package:clover_sdk/clover_sdk.dart';
```

### 2. Obtener la instancia

```dart
final cloverSdk = CloverSdkPlugin.instance;
```

### 3. Configurar callbacks

```dart
void setupCallbacks() {
  // Callback cuando el SDK se inicializa
  cloverSdk.onInitialized = (response) {
    print('SDK inicializado: ${response['success']}');
  };

  // Callback cuando se recibe respuesta de venta
  cloverSdk.onSaleResponse = (response) {
    if (response['success'] == true) {
      print('Pago exitoso: ${response['payment']}');
    } else {
      print('Error en pago: ${response['reason']}');
    }
  };

  // Callback cuando el dispositivo se conecta/desconecta
  cloverSdk.onDeviceConnected = (response) {
    print('Dispositivo conectado');
  };

  cloverSdk.onDeviceDisconnected = (response) {
    print('Dispositivo desconectado');
  };
}
```

### 4. Inicializar el SDK

```dart
Future<void> initClover() async {
  setupCallbacks();

  final result = await cloverSdk.initialize(
    // accountName: 'mi_cuenta',  // Opcional
    // accountType: 'com.clover.account',  // Opcional, por defecto
    // remoteApplicationId: 'mi_app_id',  // Opcional
  );

  if (result['success'] == true) {
    print('SDK inicializado correctamente');
  }
}
```

### 5. Realizar un pago

```dart
Future<void> realizarPago() async {
  final result = await cloverSdk.sale(
    amount: 1000,  // $10.00 en centavos
    externalId: 'order_${DateTime.now().millisecondsSinceEpoch}',
  );

  // La respuesta real llegará en el callback onSaleResponse
  print('Solicitud de pago enviada');
}
```

## 📚 Ejemplos

### Ejemplo Completo: Procesar un Pago

```dart
import 'package:flutter/material.dart';
import 'package:clover_sdk/clover_sdk.dart';

class PaymentScreen extends StatefulWidget {
  @override
  _PaymentScreenState createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  final CloverSdkPlugin _cloverSdk = CloverSdkPlugin.instance;
  String _status = 'No inicializado';
  Map<String, dynamic>? _lastPayment;

  @override
  void initState() {
    super.initState();
    _setupCallbacks();
    _initializeSDK();
  }

  void _setupCallbacks() {
    _cloverSdk.onInitialized = (response) {
      setState(() {
        _status = response['success'] == true
            ? 'SDK Inicializado'
            : 'Error al inicializar';
      });
    };

    _cloverSdk.onSaleResponse = (response) {
      setState(() {
        if (response['success'] == true) {
          _status = 'Pago Exitoso';
          _lastPayment = response['payment'];
        } else {
          _status = 'Error: ${response['reason']}';
        }
      });
    };

    _cloverSdk.onDeviceConnected = (response) {
      setState(() {
        _status = 'Dispositivo Conectado';
      });
    };

    _cloverSdk.onDeviceDisconnected = (response) {
      setState(() {
        _status = 'Dispositivo Desconectado';
      });
    };
  }

  Future<void> _initializeSDK() async {
    await _cloverSdk.initialize();
  }

  Future<void> _processPayment(double amount) async {
    final amountInCents = (amount * 100).toInt();
    final externalId = 'order_${DateTime.now().millisecondsSinceEpoch}';

    await _cloverSdk.sale(
      amount: amountInCents,
      externalId: externalId,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Clover Payment')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Estado: $_status',
                         style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    if (_lastPayment != null) ...[
                      SizedBox(height: 8),
                      Text('Último Pago:',
                           style: TextStyle(fontWeight: FontWeight.bold)),
                      Text('ID: ${_lastPayment!['id']}'),
                      Text('Monto: \$${(_lastPayment!['amount'] / 100).toStringAsFixed(2)}'),
                    ],
                  ],
                ),
              ),
            ),
            SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => _processPayment(10.00),
              child: Text('Pagar \$10.00'),
            ),
            SizedBox(height: 8),
            ElevatedButton(
              onPressed: () => _processPayment(25.50),
              child: Text('Pagar \$25.50'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _cloverSdk.disconnect();
    super.dispose();
  }
}
```

### Ejemplo: Pre-autorización y Captura

```dart
String? _preAuthPaymentId;

// 1. Realizar pre-autorización
Future<void> preAuthorize(double amount) async {
  _cloverSdk.onPreAuthResponse = (response) {
    if (response['success'] == true) {
      _preAuthPaymentId = response['payment']?['id'];
      print('Pre-autorización exitosa: $_preAuthPaymentId');
    }
  };

  await _cloverSdk.preAuth(
    amount: (amount * 100).toInt(),
    externalId: 'preauth_${DateTime.now().millisecondsSinceEpoch}',
  );
}

// 2. Capturar la pre-autorización
Future<void> capturePreAuth(double amount, double tip) async {
  if (_preAuthPaymentId == null) {
    print('No hay pre-autorización pendiente');
    return;
  }

  _cloverSdk.onCapturePreAuthResponse = (response) {
    if (response['success'] == true) {
      print('Pre-autorización capturada exitosamente');
      _preAuthPaymentId = null;
    }
  };

  await _cloverSdk.capturePreAuth(
    amount: (amount * 100).toInt(),
    paymentId: _preAuthPaymentId!,
    tipAmount: (tip * 100).toInt(),
  );
}
```

### Ejemplo: Reembolso

```dart
Future<void> refundPayment(String orderId, String paymentId, double? amount) async {
  _cloverSdk.onRefundPaymentResponse = (response) {
    if (response['success'] == true) {
      print('Reembolso exitoso');
      print('Refund ID: ${response['refund']?['id']}');
    } else {
      print('Error en reembolso: ${response['reason']}');
    }
  };

  await _cloverSdk.refundPayment(
    orderId: orderId,
    paymentId: paymentId,
    amount: amount != null ? (amount * 100).toInt() : null,
    fullRefund: amount == null,  // Si no se especifica monto, es reembolso completo
  );
}
```

### Ejemplo: Reembolso Manual

```dart
Future<void> manualRefund(double amount) async {
  _cloverSdk.onManualRefundResponse = (response) {
    if (response['success'] == true) {
      print('Reembolso manual exitoso');
    }
  };

  await _cloverSdk.manualRefund(
    amount: (amount * 100).toInt(),
    externalId: 'manual_refund_${DateTime.now().millisecondsSinceEpoch}',
  );
}
```

### Ejemplo: Anular un Pago

```dart
Future<void> voidPayment(String orderId, String paymentId) async {
  _cloverSdk.onVoidPaymentResponse = (response) {
    if (response['success'] == true) {
      print('Pago anulado exitosamente');
    }
  };

  await _cloverSdk.voidPayment(
    orderId: orderId,
    paymentId: paymentId,
    voidReason: 'Cancelado por el cliente',
  );
}
```

### Ejemplo: Consultar Pagos Pendientes

```dart
Future<void> checkPendingPayments() async {
  _cloverSdk.onRetrievePendingPaymentsResponse = (response) {
    if (response['success'] == true) {
      final pendingPayments = response['pendingPaymentEntries'] as List?;
      if (pendingPayments != null && pendingPayments.isNotEmpty) {
        print('Pagos pendientes: ${pendingPayments.length}');
        for (var payment in pendingPayments) {
          print('Payment ID: ${payment['paymentId']}, Amount: ${payment['amount']}');
        }
      } else {
        print('No hay pagos pendientes');
      }
    }
  };

  await _cloverSdk.retrievePendingPayments();
}
```

### Ejemplo: Confirmar o Rechazar un Pago

```dart
Payment? _pendingPayment;
Challenge? _pendingChallenge;

void _setupConfirmPaymentCallback() {
  _cloverSdk.onConfirmPaymentRequest = (request) {
    // Guardar el pago y challenge pendientes
    _pendingPayment = request['payment'];
    _pendingChallenge = request['challenges']?.first;

    // Mostrar diálogo al usuario
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Confirmar Pago'),
        content: Text('¿Desea confirmar este pago?'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _rejectPayment();
            },
            child: Text('Rechazar'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _confirmPayment();
            },
            child: Text('Confirmar'),
          ),
        ],
      ),
    );
  };
}

Future<void> _confirmPayment() async {
  if (_pendingPayment == null) return;

  await _cloverSdk.confirmPayment(
    payment: _pendingPayment!,
    challenges: _pendingChallenge != null ? [_pendingChallenge!] : null,
  );
}

Future<void> _rejectPayment() async {
  if (_pendingPayment == null || _pendingChallenge == null) return;

  await _cloverSdk.rejectPayment(
    payment: _pendingPayment!,
    reason: 'Rechazado por el usuario',
  );
}
```

## 📖 API Completa

### Inicialización

```dart
Future<Map<String, dynamic>> initialize({
  String? accountName,
  String accountType = 'com.clover.account',
  String? remoteApplicationId,
})
```

### Pagos

#### Sale (Venta)
```dart
Future<Map<String, dynamic>> sale({
  required int amount,              // Monto en centavos
  required String externalId,       // ID externo único
  int tipAmount = 0,                // Propina en centavos
  int taxAmount = 0,                // Impuesto en centavos
  int? tippableAmount,              // Monto sobre el cual se puede agregar propina
  String? orderId,                  // ID de la orden
  bool disablePrinting = false,     // Deshabilitar impresión
  bool disableReceiptSelection = false,
  String? signatureEntryLocation,   // "ON_SCREEN" o "ON_PAPER"
  int? signatureThreshold,          // Umbral para requerir firma
  int? cardEntryMethods,            // Métodos de entrada de tarjeta
  String? vaultedCard,              // ID de tarjeta almacenada
  bool? allowOfflinePayment,
  bool? approveOfflinePaymentWithoutPrompt,
  String? tipMode,                  // "TIP_PROVIDED", "ON_SCREEN", "ON_PAPER", "NO_TIP"
  bool? disableRestartTransactionOnFail,
})
```

#### PreAuth (Pre-autorización)
```dart
Future<Map<String, dynamic>> preAuth({
  required int amount,
  required String externalId,
  int? cardEntryMethods,
  String? vaultedCard,
})
```

#### CapturePreAuth (Capturar Pre-autorización)
```dart
Future<Map<String, dynamic>> capturePreAuth({
  required int amount,
  required String paymentId,
  int tipAmount = 0,
})
```

#### TipAdjustAuth (Ajustar Propina)
```dart
Future<Map<String, dynamic>> tipAdjustAuth({
  required String orderId,
  required String paymentId,
  required int tipAmount,
})
```

### Reembolsos

#### RefundPayment (Reembolsar Pago)
```dart
Future<Map<String, dynamic>> refundPayment({
  required String orderId,
  required String paymentId,
  int? amount,                      // Si es null, es reembolso completo
  bool fullRefund = false,
  bool disablePrinting = false,
  bool disableReceiptSelection = false,
})
```

#### ManualRefund (Reembolso Manual)
```dart
Future<Map<String, dynamic>> manualRefund({
  required int amount,
  required String externalId,
  int? cardEntryMethods,
  bool disablePrinting = false,
  bool disableReceiptSelection = false,
})
```

### Anulaciones

#### VoidPayment (Anular Pago)
```dart
Future<Map<String, dynamic>> voidPayment({
  required String orderId,
  required String paymentId,
  String? voidReason,
  bool disablePrinting = false,
  bool disableReceiptSelection = false,
})
```

#### VoidPaymentRefund (Anular Reembolso)
```dart
Future<Map<String, dynamic>> voidPaymentRefund({
  required String refundId,
  required String orderId,
  bool disablePrinting = false,
  bool disableReceiptSelection = false,
})
```

### Consultas

#### RetrievePayment (Recuperar Pago)
```dart
Future<Map<String, dynamic>> retrievePayment({
  required String externalPaymentId,
})
```

#### RetrievePendingPayments (Recuperar Pagos Pendientes)
```dart
Future<Map<String, dynamic>> retrievePendingPayments()
```

### Otros

#### Closeout (Cierre de Lote)
```dart
Future<Map<String, dynamic>> closeout({
  bool allowOpenTabs = false,
  String? batchId,
})
```

#### ReadCardData (Leer Datos de Tarjeta)
```dart
Future<Map<String, dynamic>> readCardData({
  int? cardEntryMethods,
})
```

#### ConfirmPayment (Confirmar Pago)
```dart
Future<Map<String, dynamic>> confirmPayment({
  required Map<String, dynamic> payment,
  List<Map<String, dynamic>>? challenges,
})
```

#### RejectPayment (Rechazar Pago)
```dart
Future<Map<String, dynamic>> rejectPayment({
  required Map<String, dynamic> payment,
  required String reason,
})
```

#### Disconnect (Desconectar)
```dart
Future<Map<String, dynamic>> disconnect()
```

## 🔔 Callbacks y Eventos

Todos los callbacks reciben un `Map<String, dynamic>` con la información de la respuesta:

### Callbacks de Estado

- **`onInitialized`**: Se llama cuando el SDK se inicializa correctamente
- **`onDeviceConnected`**: Se llama cuando el dispositivo Clover se conecta
- **`onDeviceDisconnected`**: Se llama cuando el dispositivo Clover se desconecta
- **`onServiceDisconnected`**: Se llama cuando el servicio se desconecta

### Callbacks de Pagos

- **`onSaleResponse`**: Respuesta de una venta (sale)
- **`onPreAuthResponse`**: Respuesta de una pre-autorización
- **`onAuthResponse`**: Respuesta de una autorización
- **`onCapturePreAuthResponse`**: Respuesta de captura de pre-autorización
- **`onTipAdjustAuthResponse`**: Respuesta de ajuste de propina
- **`onTipAdded`**: Se llama cuando se agrega una propina

### Callbacks de Reembolsos

- **`onRefundPaymentResponse`**: Respuesta de reembolso de pago
- **`onManualRefundResponse`**: Respuesta de reembolso manual
- **`onVoidPaymentRefundResponse`**: Respuesta de anulación de reembolso

### Callbacks de Anulaciones

- **`onVoidPaymentResponse`**: Respuesta de anulación de pago

### Callbacks de Consultas

- **`onRetrievePaymentResponse`**: Respuesta de recuperación de pago
- **`onRetrievePendingPaymentsResponse`**: Respuesta de pagos pendientes

### Callbacks de Otros

- **`onCloseoutResponse`**: Respuesta de cierre de lote
- **`onReadCardDataResponse`**: Respuesta de lectura de datos de tarjeta
- **`onVerifySignatureRequest`**: Solicitud de verificación de firma
- **`onConfirmPaymentRequest`**: Solicitud de confirmación de pago
- **`onVaultCardResponse`**: Respuesta de almacenamiento de tarjeta

### Estructura de Respuestas

Todas las respuestas de pago/reembolso incluyen:

```dart
{
  'success': bool,           // Si la operación fue exitosa
  'result': String,          // Resultado de la operación
  'reason': String?,         // Razón del error (si hay)
  'message': String?,        // Mensaje descriptivo
  'payment': Map?,           // Información del pago (si aplica)
  'refund': Map?,            // Información del reembolso (si aplica)
  // ... otros campos específicos según el tipo de respuesta
}
```

## ⚠️ Manejo de Errores

El SDK maneja errores de varias formas:

1. **Errores en métodos**: Los métodos retornan un `Map` con `success: false` y `error`:

```dart
final result = await _cloverSdk.sale(amount: 1000, externalId: 'test');
if (result['success'] == false) {
  print('Error: ${result['error']}');
}
```

2. **Errores en callbacks**: Los callbacks incluyen información de error:

```dart
_cloverSdk.onSaleResponse = (response) {
  if (response['success'] == false) {
    print('Error: ${response['reason']}');
    print('Mensaje: ${response['message']}');
  }
};
```

3. **Excepciones**: Algunos errores pueden lanzar excepciones, úsalos con try-catch:

```dart
try {
  await _cloverSdk.initialize();
} catch (e) {
  print('Excepción: $e');
}
```

## 🔧 Troubleshooting

### El SDK no se inicializa

- Verifica que tengas los permisos necesarios en el AndroidManifest
- Asegúrate de que el dispositivo Clover esté conectado y funcionando
- Verifica que la cuenta de Clover esté configurada correctamente

### No se reciben callbacks

- Asegúrate de configurar los callbacks antes de llamar a los métodos
- Verifica que el SDK esté inicializado correctamente
- Revisa los logs de Android para ver errores del SDK

### Errores de conexión

- Verifica que el dispositivo Clover esté encendido y conectado
- Asegúrate de que la aplicación tenga los permisos necesarios
- Intenta reinicializar el SDK llamando a `disconnect()` y luego `initialize()`

### Pagos que no se procesan

- Verifica que el monto esté en centavos (ej: $10.00 = 1000)
- Asegúrate de que el `externalId` sea único para cada transacción
- Revisa los callbacks de error para más información

## 📝 Notas Importantes

1. **Montos**: Todos los montos deben estar en **centavos** (ej: $10.00 = 1000 centavos)

2. **External IDs**: Los `externalId` deben ser únicos. Usa timestamps o UUIDs para generarlos

3. **Callbacks**: Los callbacks deben configurarse **antes** de llamar a los métodos que los generan

4. **Threading**: El SDK maneja automáticamente el threading, pero asegúrate de actualizar la UI desde el hilo principal

5. **Lifecycle**: Siempre llama a `disconnect()` cuando termines de usar el SDK (por ejemplo, en `dispose()`)

6. **Confirmación de Pagos**: Algunos pagos pueden requerir confirmación. Usa `onConfirmPaymentRequest` para manejar esto

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📧 Soporte

Para reportar bugs o solicitar features, por favor abre un issue en el repositorio.

## 🙏 Agradecimientos

- Clover Network por el SDK oficial de Android
- La comunidad de Flutter por las herramientas y recursos

---

**Desarrollado con ❤️ para la comunidad Flutter**
