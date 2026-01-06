# Clover SDK para Flutter

[![Version](https://img.shields.io/badge/version-0.3.7-blue.svg)](https://pub.dev/packages/clover_sdk)
[![Flutter](https://img.shields.io/badge/flutter-%3E%3D3.3.0-blue.svg)](https://flutter.dev)

Plugin de Flutter para integrar el SDK de Clover en aplicaciones Android, permitiendo procesar pagos con dispositivos Clover.

> **Nota**: Este plugin está simplificado para procesar pagos únicamente. Para funcionalidades avanzadas, consulta la [documentación oficial de Clover](https://docs.clover.com/dev/docs/take-a-payment-with-payment-connector).

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso Básico](#-uso-básico)
- [Ejemplo Completo](#-ejemplo-completo)
- [API](#-api)
- [Callbacks](#-callbacks)
- [Troubleshooting](#-troubleshooting)

## ✨ Características

- ✅ **Procesar Pagos**: Realizar pagos con tarjeta usando dispositivos Clover
- ✅ **Pagos con QR**: Mostrar código QR para que el cliente escanee y realice el pago
- ✅ **Arquitectura Limpia**: Código organizado en capas (models, services, mappers)
- ✅ **Callbacks Completos**: Sistema de eventos para manejar respuestas
- ✅ **Modo Kiosco**: Bloquear el sistema para aplicaciones POS/kiosco
- ✅ **Modo Inmersivo**: Ocultar barras del sistema para pantalla completa
- ✅ **Basado en Documentación Oficial**: Implementación siguiendo las mejores prácticas de Clover

## 📋 Requisitos

- Flutter >= 3.3.0
- Android minSdkVersion 21
- Dispositivo Clover o Clover Dev Kit
- Remote Application ID (RAID) de tu aplicación Clover

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

El plugin requiere permisos específicos de Clover. Estos ya están incluidos en el plugin, pero asegúrate de que tu aplicación tenga los permisos necesarios en el `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <!-- Permisos necesarios para el SDK de Clover -->
  <uses-permission android:name="com.clover.permission.LAUNCH_PAYMENTS" />
  <uses-permission android:name="com.clover.permission.READ_MERCHANT" />
  <uses-permission android:name="android.permission.GET_ACCOUNTS" />
  <uses-permission android:name="android.permission.INTERNET" />

  <!-- ... resto de tu manifest ... -->
</manifest>
```

### Obtener Remote Application ID (RAID)

1. Ve al [Clover Developer Dashboard](https://www.clover.com/developers)
2. Crea una nueva aplicación o selecciona una existente
3. Copia el **Remote Application ID** (RAID) de la configuración de tu app
4. Este ID es necesario para inicializar el SDK

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

  // Callback cuando el dispositivo se conecta
  cloverSdk.onDeviceConnected = (response) {
    print('Dispositivo conectado');
  };

  // Callback cuando el dispositivo se desconecta
  cloverSdk.onDeviceDisconnected = (response) {
    print('Dispositivo desconectado');
  };

  // Callback cuando se recibe respuesta de pago
  cloverSdk.onSaleResponse = (response) {
    if (response['success'] == true) {
      print('Pago exitoso!');
      print('Payment ID: ${response['payment']?['id']}');
      print('Monto: ${response['payment']?['amount']}');
    } else {
      print('Error en pago: ${response['reason']}');
      print('Mensaje: ${response['message']}');
    }
  };

  // Callback cuando se recibe respuesta de pago QR
  cloverSdk.onQrPaymentResponse = (response) {
    if (response['success'] == true) {
      print('Pago QR exitoso!');
      print('Payment ID: ${response['payment']?['id']}');
      print('Monto: ${response['payment']?['amount']}');
    } else {
      print('Error en pago QR: ${response['reason']}');
      print('Mensaje: ${response['message']}');
    }
  };
}
```

### 4. Inicializar el SDK

```dart
Future<void> initClover() async {
  setupCallbacks();

  final result = await cloverSdk.initialize(
    remoteApplicationId: 'TU_RAID_AQUI', // Reemplaza con tu RAID
  );

  if (result['success'] == true) {
    print('SDK inicializado correctamente');
  } else {
    print('Error: ${result['error']}');
  }
}
```

### 5. Mantener la pantalla encendida (Opcional)

```dart
// Mantener la pantalla encendida (útil para aplicaciones POS)
await cloverSdk.keepScreenOn(keepOn: true);

// O liberar el flag para permitir que se apague
await cloverSdk.releaseScreenOn();
```

### 6. Modo Inmersivo - Ocultar barras del sistema (Opcional)

```dart
// Ocultar barra de estado y barra de navegación (modo kiosco)
await cloverSdk.setImmersiveMode(
  hideStatusBar: true,
  hideNavigationBar: true,
);

// O solo ocultar la barra de estado
await cloverSdk.setImmersiveMode(
  hideStatusBar: true,
  hideNavigationBar: false,
);

// Restaurar las barras del sistema
await cloverSdk.exitImmersiveMode();
```

### 7. Modo Kiosco - Bloquear el sistema (Opcional)

```dart
// Activar modo kiosco con código de desbloqueo
await cloverSdk.enableKioskMode(
  unlockCode: 'MI_CODIGO_SECRETO',
  enableScreenPinning: true,
);

// Verificar si el modo kiosco está activo
final status = await cloverSdk.isKioskModeActive();
print('Modo kiosco activo: ${status['isActive']}');

// Desactivar modo kiosco (requiere el código de desbloqueo)
await cloverSdk.disableKioskMode(unlockCode: 'MI_CODIGO_SECRETO');
```

**⚠️ IMPORTANTE sobre el Modo Kiosco:**
- Bloquea los botones HOME, RECENT APPS, MENU y BACK
- Requiere Android 5.0+ (API 21+)
- En algunos dispositivos puede requerir que la app sea configurada como administrador del dispositivo
- Para salir del modo kiosco, debes llamar a `disableKioskMode()` con el código correcto
- Recomendado para aplicaciones POS/kiosco donde necesitas control total

### 8. Procesar un pago con tarjeta

```dart
Future<void> realizarPago() async {
  // Importante: Esperar a que el dispositivo esté conectado
  // antes de procesar pagos

  final result = await cloverSdk.sale(
    amount: 1000,  // $10.00 en centavos
    externalId: 'order_${DateTime.now().millisecondsSinceEpoch}',
  );

  // La respuesta real llegará en el callback onSaleResponse
  print('Solicitud de pago enviada');
}
```

### 9. Procesar un pago con QR

```dart
// Configurar callback para recibir respuesta del pago QR
cloverSdk.onQrPaymentResponse = (response) {
  if (response['success'] == true) {
    print('Pago QR exitoso!');
    print('Payment ID: ${response['payment']?['id']}');
  } else {
    print('Error en pago QR: ${response['reason']}');
  }
};

// Presentar QR para que el cliente escanee
Future<void> realizarPagoQR() async {
  final result = await cloverSdk.presentQrCode(
    amount: 1000,  // $10.00 en centavos
    externalId: 'order_${DateTime.now().millisecondsSinceEpoch}',
    orderId: 'optional_order_id', // Opcional
  );

  // El QR se mostrará en la pantalla del dispositivo Clover
  // El cliente puede escanearlo con su app de pago (Mercado Pago, PayPal, etc.)
  // La respuesta llegará en el callback onQrPaymentResponse
  print('QR Code presentado. Esperando que el cliente escanee.');
}
```

**Nota sobre Pagos QR:**
- El QR se muestra en la pantalla del dispositivo Clover
- El cliente escanea el QR con su app de pago (Mercado Pago, PayPal, Venmo, etc.)
- La respuesta del pago llegará en el callback `onQrPaymentResponse`
- No requiere que el dispositivo esté conectado (a diferencia de los pagos con tarjeta)

## 📚 Ejemplo Completo

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
  bool _deviceConnected = false;
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

    _cloverSdk.onDeviceConnected = (response) {
      setState(() {
        _deviceConnected = true;
        _status = 'Dispositivo Conectado - Listo para pagos';
      });
    };

    _cloverSdk.onDeviceDisconnected = (response) {
      setState(() {
        _deviceConnected = false;
        _status = 'Dispositivo Desconectado';
      });
    };

    _cloverSdk.onSaleResponse = (response) {
      setState(() {
        if (response['success'] == true) {
          _status = 'Pago Exitoso';
          _lastPayment = response['payment'];
        } else {
          _status = 'Error: ${response['reason']} - ${response['message']}';
        }
      });
    };

    _cloverSdk.onQrPaymentResponse = (response) {
      setState(() {
        if (response['success'] == true) {
          _status = 'Pago QR Exitoso';
          _lastPayment = response['payment'];
        } else {
          _status = 'Error QR: ${response['reason']} - ${response['message']}';
        }
      });
    };
  }

  Future<void> _initializeSDK() async {
    // Reemplaza 'TU_RAID_AQUI' con tu Remote Application ID
    await _cloverSdk.initialize(
      remoteApplicationId: 'TU_RAID_AQUI',
    );

    // Opcional: Mantener la pantalla encendida para aplicaciones POS
    await _cloverSdk.keepScreenOn(keepOn: true);
  }

  Future<void> _processPayment(double amount) async {
    if (!_deviceConnected) {
      setState(() {
        _status = 'Error: Dispositivo no conectado';
      });
      return;
    }

    final amountInCents = (amount * 100).toInt();
    final externalId = 'order_${DateTime.now().millisecondsSinceEpoch}';

    setState(() {
      _status = 'Procesando pago...';
    });

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
                    Row(
                      children: [
                        Icon(
                          _deviceConnected ? Icons.check_circle : Icons.error,
                          color: _deviceConnected ? Colors.green : Colors.red,
                        ),
                        SizedBox(width: 8),
                        Text(
                          _status,
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    if (_lastPayment != null) ...[
                      SizedBox(height: 16),
                      Divider(),
                      SizedBox(height: 8),
                      Text(
                        'Último Pago:',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text('ID: ${_lastPayment!['id']}'),
                      Text('Monto: \$${(_lastPayment!['amount']! / 100).toStringAsFixed(2)}'),
                      if (_lastPayment!['tipAmount'] != null)
                        Text('Propina: \$${(_lastPayment!['tipAmount']! / 100).toStringAsFixed(2)}'),
                    ],
                  ],
                ),
              ),
            ),
            SizedBox(height: 16),
            ElevatedButton(
              onPressed: _deviceConnected ? () => _processPayment(10.00) : null,
              child: Text('Pagar \$10.00'),
            ),
            SizedBox(height: 8),
            ElevatedButton(
              onPressed: _deviceConnected ? () => _processPayment(25.50) : null,
              child: Text('Pagar \$25.50'),
            ),
            SizedBox(height: 8),
            ElevatedButton(
              onPressed: _deviceConnected ? () => _processPayment(50.00) : null,
              child: Text('Pagar \$50.00'),
            ),
            SizedBox(height: 8),
            ElevatedButton(
              onPressed: () => _processQrPayment(10.00),
              child: Text('Pagar con QR \$10.00'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _processQrPayment(double amount) async {
    final amountInCents = (amount * 100).toInt();
    final externalId = 'qr_order_${DateTime.now().millisecondsSinceEpoch}';

    setState(() {
      _status = 'Mostrando QR Code...';
    });

    await _cloverSdk.presentQrCode(
      amount: amountInCents,
      externalId: externalId,
    );
  }

  @override
  void dispose() {
    _cloverSdk.dispose();
    super.dispose();
  }
}
```

## 📖 API

### `initialize({required String remoteApplicationId})`

Inicializa el SDK de Clover.

**Parámetros:**
- `remoteApplicationId` (requerido): El Remote Application ID (RAID) de tu aplicación Clover

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se inicializó correctamente
- `error`: Mensaje de error si falló

### `sale({required int amount, required String externalId})`

Procesa un pago.

**Parámetros:**
- `amount` (requerido): Monto en centavos. Ejemplo: $10.00 = 1000 centavos
- `externalId` (requerido): ID externo único para la transacción. Debe ser único para cada pago.

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si la solicitud se envió correctamente
- `message`: Mensaje descriptivo

**Nota:** La respuesta real del pago llegará en el callback `onSaleResponse`.

### `dispose()`

Desconecta y libera recursos del SDK.

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se desconectó correctamente

### `keepScreenOn({bool keepOn = true})`

Mantiene la pantalla encendida o la libera.

**Parámetros:**
- `keepOn` (opcional): Si es `true`, mantiene la pantalla encendida. Si es `false`, permite que se apague (por defecto: `true`)

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se configuró correctamente
- `message`: Mensaje descriptivo

**Nota:** Útil para aplicaciones POS donde necesitas mantener la pantalla activa durante las transacciones. La pantalla se mantendrá encendida hasta que llames a `releaseScreenOn()` o `dispose()`.

### `releaseScreenOn()`

Libera el flag que mantiene la pantalla encendida, permitiendo que la pantalla se apague normalmente según la configuración del sistema.

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se liberó correctamente

### `setImmersiveMode({bool hideStatusBar = true, bool hideNavigationBar = true})`

Activa el modo inmersivo para ocultar la barra de estado y/o la barra de navegación.

**Parámetros:**
- `hideStatusBar` (opcional): Si es `true`, oculta la barra de estado (por defecto: `true`)
- `hideNavigationBar` (opcional): Si es `true`, oculta la barra de navegación (por defecto: `true`)

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se configuró correctamente
- `message`: Mensaje descriptivo

**Nota:**
- El modo inmersivo oculta las barras del sistema permanentemente
- Las barras se muestran temporalmente cuando el usuario desliza desde los bordes
- Útil para aplicaciones kiosco/POS donde necesitas pantalla completa
- Compatible con Android 5.0+ (API 21+)

### `exitImmersiveMode()`

Desactiva el modo inmersivo y restaura las barras del sistema (estado y navegación) a su estado normal.

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se desactivó correctamente
- `message`: Mensaje descriptivo

### `enableKioskMode({String? unlockCode, bool enableScreenPinning = true})`

Activa el modo kiosco para bloquear el sistema y prevenir que se salga de la app.

**Parámetros:**
- `unlockCode` (opcional): Código requerido para desactivar el modo kiosco (altamente recomendado)
- `enableScreenPinning` (opcional): Si es `true`, activa Screen Pinning/Lock Task Mode (por defecto: `true`)

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se activó correctamente
- `message`: Mensaje descriptivo

**Características:**
- Bloquea los botones HOME, RECENT APPS, MENU y BACK
- Previene que se abran otras aplicaciones
- Requiere Android 5.0+ (API 21+)
- En algunos dispositivos puede requerir configuración adicional del administrador del dispositivo

**⚠️ ADVERTENCIA:** Una vez activado, solo puedes salir llamando a `disableKioskMode()` con el código correcto. Asegúrate de tener una forma de desactivarlo.

### `disableKioskMode({String? unlockCode})`

Desactiva el modo kiosco y restaura la funcionalidad normal del sistema.

**Parámetros:**
- `unlockCode` (opcional): Código de desbloqueo (requerido si se configuró al activar)

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si se desactivó correctamente
- `message`: Mensaje descriptivo

### `isKioskModeActive()`

Verifica si el modo kiosco está actualmente activo.

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si la verificación fue exitosa
- `isActive`: `true` si el modo kiosco está activo, `false` en caso contrario

### `presentQrCode({required int amount, required String externalId, String? orderId})`

Presenta un código QR para que el cliente escanee y realice el pago.

**Parámetros:**
- `amount` (requerido): Monto en centavos. Ejemplo: $10.00 = 1000 centavos
- `externalId` (requerido): ID externo único para la transacción. Debe ser único para cada pago.
- `orderId` (opcional): ID de la orden en Clover

**Retorna:** `Future<Map<String, dynamic>>` con:
- `success`: `true` si el QR se presentó correctamente
- `message`: Mensaje descriptivo

**Nota:**
- Muestra un código QR en la pantalla del dispositivo Clover
- El cliente puede escanear el QR con su app de pago (Mercado Pago, PayPal, Venmo, etc.)
- La respuesta real del pago llegará en el callback `onQrPaymentResponse`
- No requiere que el dispositivo esté conectado (a diferencia de los pagos con tarjeta)
- El QR se genera automáticamente por el sistema Clover

## 🔔 Callbacks

### `onInitialized`

Se llama cuando el SDK se inicializa correctamente.

```dart
cloverSdk.onInitialized = (response) {
  // response['success'] == true
  // response['message'] == "SDK inicializado correctamente"
};
```

### `onDeviceConnected`

Se llama cuando el dispositivo Clover se conecta.

```dart
cloverSdk.onDeviceConnected = (response) {
  // response['success'] == true
  // response['message'] == "Dispositivo conectado"
  // Ahora puedes procesar pagos
};
```

### `onDeviceDisconnected`

Se llama cuando el dispositivo Clover se desconecta.

```dart
cloverSdk.onDeviceDisconnected = (response) {
  // response['success'] == false
  // response['message'] == "Dispositivo desconectado"
};
```

### `onSaleResponse`

Se llama cuando se recibe la respuesta de un pago con tarjeta.

```dart
cloverSdk.onSaleResponse = (response) {
  if (response['success'] == true) {
    // Pago exitoso
    final payment = response['payment'];
    // payment['id'] - ID del pago
    // payment['amount'] - Monto en centavos
    // payment['tipAmount'] - Propina en centavos (si aplica)
    // payment['externalPaymentId'] - ID externo del pago
    // payment['orderId'] - ID de la orden
  } else {
    // Error en el pago
    // response['reason'] - Razón del error
    // response['message'] - Mensaje descriptivo
  }
};
```

### `onQrPaymentResponse`

Se llama cuando se recibe la respuesta de un pago con QR.

```dart
cloverSdk.onQrPaymentResponse = (response) {
  if (response['success'] == true) {
    // Pago QR exitoso
    final payment = response['payment'];
    // payment['id'] - ID del pago
    // payment['amount'] - Monto en centavos
    // payment['tipAmount'] - Propina en centavos (si aplica)
    // payment['externalPaymentId'] - ID externo del pago
    // payment['orderId'] - ID de la orden
    // response['qrCodeData'] - Datos del código QR (si están disponibles)
  } else {
    // Error en el pago QR
    // response['reason'] - Razón del error
    // response['message'] - Mensaje descriptivo
  }
};
```

**Nota:** Este callback se activa cuando el cliente completa el pago escaneando el QR. La respuesta puede llegar a través de un BroadcastReceiver o callback del sistema Clover.

## ⚠️ Troubleshooting

### El SDK no se inicializa

- Verifica que tengas el **Remote Application ID (RAID)** correcto
- Asegúrate de que los permisos estén en el `AndroidManifest.xml`
- Verifica que el dispositivo Clover esté encendido y funcionando

### No se recibe el callback `onDeviceConnected`

- Espera unos segundos después de inicializar el SDK
- Verifica que el dispositivo Clover esté conectado a la red
- Revisa los logs de Android para ver errores del SDK

### El pago no se procesa

- **Importante**: Para pagos con tarjeta, espera a que `onDeviceConnected` se llame antes de procesar pagos
- Para pagos con QR, no es necesario esperar la conexión del dispositivo
- Verifica que el monto esté en **centavos** (ej: $10.00 = 1000)
- Asegúrate de que el `externalId` sea único para cada transacción
- Revisa el callback correspondiente (`onSaleResponse` o `onQrPaymentResponse`) para ver el error específico

### El QR no se muestra

- Verifica que el servicio de pagos QR esté disponible en el dispositivo
- Asegúrate de que la app Clover esté instalada y actualizada
- Revisa los logs de Android para ver errores específicos

### Error "NOT_INITIALIZED"

- Asegúrate de llamar a `initialize()` antes de procesar pagos
- Verifica que el `remoteApplicationId` sea correcto

## 📝 Notas Importantes

1. **Montos**: Todos los montos deben estar en **centavos** (ej: $10.00 = 1000 centavos)

2. **External IDs**: Los `externalId` deben ser únicos. Usa timestamps o UUIDs:
   ```dart
   final externalId = 'order_${DateTime.now().millisecondsSinceEpoch}';
   // o
   final externalId = Uuid().v4();
   ```

3. **Esperar Conexión**: Siempre espera a que `onDeviceConnected` se llame antes de procesar pagos

4. **Callbacks**: Los callbacks deben configurarse **antes** de llamar a `initialize()`

5. **Lifecycle**: Siempre llama a `dispose()` cuando termines de usar el SDK (por ejemplo, en `dispose()` del widget)

6. **Threading**: El SDK maneja automáticamente el threading, pero asegúrate de actualizar la UI desde el hilo principal usando `setState()`

## 🏗️ Arquitectura

El plugin está organizado en capas:

```
android/src/main/kotlin/ar/com/orderfast/
├── CloverSdkPlugin.kt          # Plugin principal (comunicación Flutter)
├── models/
│   ├── PaymentModels.kt        # Modelos de datos para pagos con tarjeta
│   └── QrPaymentModels.kt      # Modelos de datos para pagos con QR
├── services/
│   ├── PaymentService.kt       # Servicio de pagos con tarjeta (PaymentConnector)
│   ├── QrPaymentService.kt     # Servicio de pagos con QR (PayIntent)
│   ├── KioskService.kt         # Servicio de modo kiosco
│   └── ImmersiveModeService.kt # Servicio de modo inmersivo
└── mappers/
    └── PaymentMapper.kt        # Conversión entre objetos Clover y modelos
```

### Separación de Responsabilidades

- **CloverSdkPlugin**: Maneja la comunicación Flutter ↔ Native a través de MethodChannel
- **Services**: Contienen la lógica de negocio específica de cada funcionalidad
- **Models**: Representan los datos de forma independiente del SDK de Clover
- **Mappers**: Convierten entre objetos del SDK de Clover y modelos internos

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

## 📚 Referencias

- [Documentación Oficial de Clover](https://docs.clover.com/dev/docs/take-a-payment-with-payment-connector)
- [Clover Developer Dashboard](https://www.clover.com/developers)
- [Clover Android SDK](https://github.com/clover/clover-android-sdk)

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

**Desarrollado con ❤️ para la comunidad Flutter**
