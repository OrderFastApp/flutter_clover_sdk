import 'package:flutter/material.dart';
import 'package:clover_sdk/clover_sdk.dart';
import 'package:clover_sdk/models/models.dart';

/// PNG 1×1 transparente (mínimo) para probar [CloverSdkPlugin.printImage].
const String _kTinyPngBase64 =
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final CloverSdkPlugin _cloverSdk = CloverSdkPlugin.instance;
  String _status = 'No inicializado';
  bool _deviceConnected = false;
  Map<String, dynamic>? _lastPayment;
  Map<String, dynamic>? _lastQrPayment;

  static const Map<String, dynamic> _sampleOrderForPrint = {
    'id': 'order-demo-8912',
    'slug': 'orderfast-obelisco',
    'store_name': 'OrderFast Obelisco',
    'created_at': '2026-07-28T11:20:00.000Z',
    'total': 18500.0,
    'order_items': [
      {
        'name': 'Hamburguesa Quimera Doble',
        'price': 6000.0,
        'quantity': 2,
        'comment': 'Punto de la carne: A punto',
        'order_sub_selection': [
          {'name': 'Extra Queso Cheddar', 'quantity': 2, 'price': 1000.0},
          {'name': 'Sin Cebolla', 'quantity': 1, 'price': 0.0},
        ],
      },
      {
        'name': 'Papas Rústicas Grandes',
        'price': 3500.0,
        'quantity': 1,
        'comment': 'Extra picante',
        'order_sub_selection': [
          {'name': 'Salsa Cheddar & Bacon', 'quantity': 1, 'price': 1000.0},
        ],
      },
      {
        'name': 'Gaseosa Coca-Cola 500ml',
        'price': 1500.0,
        'quantity': 2,
        'order_sub_selection': <Map<String, dynamic>>[],
      },
    ],
  };

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
            : 'Error al inicializar: ${response['error']}';
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
          _status = 'Pago Exitoso (sale)';
          _lastPayment = response['payment'] != null
              ? Map<String, dynamic>.from(response['payment'] as Map)
              : null;
        } else {
          _status =
              'Sale: ${response['reason']} — ${response['message'] ?? ''}';
        }
      });
    };

    _cloverSdk.onQrPaymentResponse = (response) {
      setState(() {
        _lastQrPayment = Map<String, dynamic>.from(response);
        if (response['success'] == true) {
          _status = 'Pago QR completado';
        } else {
          _status =
              'QR: ${response['reason'] ?? response['message'] ?? response}';
        }
      });
    };
  }

  Future<void> _initializeSDK() async {
    final result = await _cloverSdk.initialize(
      remoteApplicationId: 'TU_RAID_AQUI',
    );

    if (result['success'] != true) {
      setState(() {
        _status = 'Error al inicializar: ${result['error']}';
      });
      return;
    }

    await _cloverSdk.keepScreenOn(keepOn: true);
    await _cloverSdk.setImmersiveMode(
      hideStatusBar: true,
      hideNavigationBar: true,
    );
  }

  final GlobalKey<ScaffoldMessengerState> _scaffoldMessengerKey =
      GlobalKey<ScaffoldMessengerState>();

  void _toast(String msg) {
    _scaffoldMessengerKey.currentState?.showSnackBar(
      SnackBar(content: Text(msg), duration: const Duration(seconds: 2)),
    );
  }

  Future<void> _invokeMap(
      String label, Future<Map<String, dynamic>> call) async {
    final r = await call;
    if (!mounted) return;
    final ok = r['success'] == true;
    final detail = ok
        ? (r.keys
            .where((k) => k != 'success')
            .map((k) => '$k: ${r[k]}')
            .join(', '))
        : '${r['error'] ?? r}';
    setState(() {
      _status =
          '$label → ${ok ? 'OK' : 'Error'}${detail.isNotEmpty ? ': $detail' : ''}';
    });
    _toast('$label: ${ok ? 'OK' : 'falló'}');
  }

  Future<void> _invokePrint(
      String label, Future<PrintTicketResponse> call) async {
    final r = await call;
    if (!mounted) return;
    setState(() {
      _status = '$label → ${r.success ? 'OK' : 'Error: ${r.error ?? ''}'}';
    });
    _toast('$label: ${r.success ? 'OK' : (r.error ?? 'error')}');
  }

  Future<void> _processPayment(double amount) async {
    if (!_deviceConnected) {
      setState(() {
        _status = 'Error: Dispositivo no conectado. Espera a que se conecte.';
      });
      return;
    }

    final amountInCents = (amount * 100).toInt();
    final externalId = 'order_${DateTime.now().millisecondsSinceEpoch}';

    setState(() {
      _status = 'Procesando pago de ${amount.toStringAsFixed(2)} USD...';
    });

    await _cloverSdk.sale(
      amount: amountInCents,
      externalId: externalId,
    );
  }

  Future<void> _printCashTicketSample() async {
    final ts = DateTime.now().toIso8601String();
    await _invokePrint(
      'printCashTicket',
      _cloverSdk.printCashTicket(
        nonFiscalTicket: {
          'orderNumber': 'CASH',
          'items': <Map<String, dynamic>>[],
          'total': 85.0,
          'dateTime': ts,
          'subtotalBeforeDiscount': 100.0,
          'discountAppliedAmount': 15.0,
          'discountCode': 'PROMO',
        },
      ),
    );
  }

  Future<void> _printKitchenTicketSample() async {
    final ts = DateTime.now().toIso8601String();
    final kitchenTicket = KitchenTicketDto(
      orderNumber: 'A-73',
      dateTime: ts,
      isTakeAway: true,
      identifier: 'Mesa 4',
      notes: '🚨 URGENTE: Cliente alérgico a las nueces. Servir bebidas primero.',
      items: [
        TicketItem(
          quantity: 2,
          description: 'Hamburguesa Quimera Doble',
          subtotal: 12000.0,
          total: 12000.0,
          subselections: [
            TicketSubselection(
              name: 'Sin cebolla',
              quantity: 1,
              price: 0.0,
              total: 0.0,
            ),
            TicketSubselection(
              name: 'Extra Queso Cheddar',
              quantity: 2,
              price: 1000.0,
              total: 2000.0,
            ),
          ],
        ),
        TicketItem(
          quantity: 1,
          description: 'Papas Rústicas Grandes',
          subtotal: 4500.0,
          total: 4500.0,
          subselections: [
            TicketSubselection(
              name: 'Salsa Cheddar & Bacon',
              quantity: 1,
              price: 1000.0,
              total: 1000.0,
            ),
          ],
        ),
        TicketItem(
          quantity: 2,
          description: 'Cerveza IPA 500ml',
          subtotal: 4000.0,
          total: 4000.0,
        ),
      ],
    );
    await _invokePrint(
      'printKitchenTicket',
      _cloverSdk.printKitchenTicket(kitchenTicket),
    );
  }

  Future<void> _presentQr(double dollars) async {
    if (!_deviceConnected) {
      setState(() => _status = 'QR: dispositivo no conectado');
      return;
    }
    final cents = (dollars * 100).toInt();
    await _invokeMap(
      'presentQrCode',
      _cloverSdk.presentQrCode(
        amount: cents,
        externalId: 'qr_${DateTime.now().millisecondsSinceEpoch}',
      ),
    );
  }

  Future<void> _dialogDreamComponent() async {
    final pkgCtrl = TextEditingController();
    final clsCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('setScreensaverDreamComponent'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: pkgCtrl,
              decoration: const InputDecoration(labelText: 'packageName'),
            ),
            TextField(
              controller: clsCtrl,
              decoration: const InputDecoration(labelText: 'className'),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancelar')),
          FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Enviar')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    if (pkgCtrl.text.isEmpty || clsCtrl.text.isEmpty) {
      _toast('Completá package y class');
      return;
    }
    await _invokeMap(
      'setScreensaverDreamComponent',
      _cloverSdk.setScreensaverDreamComponent(
        packageName: pkgCtrl.text.trim(),
        className: clsCtrl.text.trim(),
      ),
    );
  }

  Widget _printCard({
    required String title,
    required String templateName,
    required String description,
    required String mode,
    required VoidCallback onPressed,
  }) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      color: Colors.grey.shade50,
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: Colors.grey.shade300),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: mode.contains('Gráfico')
                        ? Colors.blue.shade100
                        : (mode.contains('Imagen')
                            ? Colors.purple.shade100
                            : Colors.orange.shade100),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    mode,
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: mode.contains('Gráfico')
                          ? Colors.blue.shade900
                          : (mode.contains('Imagen')
                              ? Colors.purple.shade900
                              : Colors.orange.shade900),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'Plantilla Nativa: $templateName',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              description,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade700,
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: onPressed,
                icon: const Icon(Icons.print, size: 18),
                label: Text('Probar $title'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _section(String title, List<Widget> children) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
            ),
            const SizedBox(height: 8),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _btn(String text, VoidCallback? onPressed) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: OutlinedButton(onPressed: onPressed, child: Text(text)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      scaffoldMessengerKey: _scaffoldMessengerKey,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Clover SDK Example'),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            _deviceConnected ? Icons.check_circle : Icons.error,
                            color: _deviceConnected ? Colors.green : Colors.red,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _status,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (_lastPayment != null) ...[
                        const SizedBox(height: 12),
                        const Divider(),
                        const Text('Último pago (sale):',
                            style: TextStyle(fontWeight: FontWeight.bold)),
                        Text('ID: ${_lastPayment!['id']}'),
                        Text(
                          'Monto: ${(((_lastPayment!['amount'] as num?) ?? 0) / 100).toStringAsFixed(2)}',
                        ),
                        if (_lastPayment!['tipAmount'] != null)
                          Text(
                            'Propina: ${(((_lastPayment!['tipAmount'] as num?) ?? 0) / 100).toStringAsFixed(2)}',
                          ),
                      ],
                      if (_lastQrPayment != null) ...[
                        const SizedBox(height: 12),
                        const Divider(),
                        const Text('Última respuesta QR:',
                            style: TextStyle(fontWeight: FontWeight.bold)),
                        Text(_lastQrPayment.toString(),
                            style: const TextStyle(fontSize: 12)),
                      ],
                    ],
                  ),
                ),
              ),
              _section('Pagos (requiere dispositivo conectado)', [
                _btn('Pagar 10.00 USD',
                    _deviceConnected ? () => _processPayment(10) : null),
                _btn('Pagar 25.50 USD',
                    _deviceConnected ? () => _processPayment(25.5) : null),
                _btn('Pagar 50.00 USD',
                    _deviceConnected ? () => _processPayment(50) : null),
                _btn('Presentar QR 5.00 USD',
                    _deviceConnected ? () => _presentQr(5) : null),
              ]),
              _section('Pantalla / inmersivo', [
                _btn(
                    'keepScreenOn(true)',
                    () => _invokeMap(
                        'keepScreenOn', _cloverSdk.keepScreenOn(keepOn: true))),
                _btn(
                    'keepScreenOn(false)',
                    () => _invokeMap('keepScreenOn',
                        _cloverSdk.keepScreenOn(keepOn: false))),
                _btn(
                    'releaseScreenOn',
                    () => _invokeMap(
                        'releaseScreenOn', _cloverSdk.releaseScreenOn())),
                _btn(
                    'setImmersiveMode (ocultar barras)',
                    () => _invokeMap(
                          'setImmersiveMode',
                          _cloverSdk.setImmersiveMode(
                            hideStatusBar: true,
                            hideNavigationBar: true,
                          ),
                        )),
                _btn(
                    'exitImmersiveMode',
                    () => _invokeMap(
                        'exitImmersiveMode', _cloverSdk.exitImmersiveMode())),
              ]),
              _section('Modo kiosco (¡cuidado en dispositivo real!)', [
                _btn(
                    'enableKioskMode',
                    () => _invokeMap(
                        'enableKioskMode', _cloverSdk.enableKioskMode())),
                _btn(
                    'disableKioskMode',
                    () => _invokeMap(
                        'disableKioskMode', _cloverSdk.disableKioskMode())),
                _btn(
                    'isKioskModeActive',
                    () => _invokeMap(
                        'isKioskModeActive', _cloverSdk.isKioskModeActive())),
              ]),
              _section('Screensaver', [
                _btn(
                    'enableScreensaver',
                    () => _invokeMap(
                          'enableScreensaver',
                          _cloverSdk.enableScreensaver(activateOnSleep: true),
                        )),
                _btn(
                    'disableScreensaver',
                    () => _invokeMap(
                        'disableScreensaver', _cloverSdk.disableScreensaver())),
                _btn(
                    'startScreensaver',
                    () => _invokeMap(
                        'startScreensaver', _cloverSdk.startScreensaver())),
                _btn(
                    'isScreensaverEnabled',
                    () => _invokeMap('isScreensaverEnabled',
                        _cloverSdk.isScreensaverEnabled())),
                _btn(
                    'isScreensaverSupported',
                    () => _invokeMap('isScreensaverSupported',
                        _cloverSdk.isScreensaverSupported())),
                _btn('setScreensaverDreamComponent…', _dialogDreamComponent),
              ]),
              _section('Pruebas de Impresión por Plantilla Nativa', [
                _printCard(
                  title: '🧾 1. Ticket de Cliente / Venta',
                  templateName: 'CustomerTicketTemplate.kt',
                  description:
                      'Se entrega al cliente tras la compra. Incluye detalle de ítems, modificadores, subtotal, descuentos y total.',
                  mode: 'Gráfico (ViewPrintJob)',
                  onPressed: () => _invokePrint('printTestNonFiscalTicket',
                      _cloverSdk.printTestNonFiscalTicket()),
                ),
                _printCard(
                  title: '🍳 2. Comanda de Cocina (DTO)',
                  templateName: 'KitchenTicketTemplate.kt',
                  description:
                      'Se envía a la cocina. Destaca el badge negro invertido con el turno/mesa, comentarios de preparación y notas.',
                  mode: 'Gráfico (ViewPrintJob)',
                  onPressed: _printKitchenTicketSample,
                ),
                _printCard(
                  title: '🏛️ 3. Factura Fiscal AFIP',
                  templateName: 'FiscalTicketTemplate.kt',
                  description:
                      'Comprobante legal impositivo. Incluye Razón Social, CUIT, Condición IVA, CAE, vencimiento y Código QR AFIP.',
                  mode: 'Gráfico (ViewPrintJob)',
                  onPressed: () => _invokePrint('printTestFiscalTicket',
                      _cloverSdk.printTestFiscalTicket()),
                ),
                _printCard(
                  title: '💵 4. Comprobante de Caja',
                  templateName: 'CashTicketTemplate.kt',
                  description:
                      'Formato compacto utilizado para aperturas/cierres de turno o resúmenes simplificados de caja.',
                  mode: 'Gráfico (ViewPrintJob)',
                  onPressed: _printCashTicketSample,
                ),
                _printCard(
                  title: '🔄 5. Conversión de Orden JSON',
                  templateName: 'orderToNonFiscalTicket() -> CustomerTicketTemplate',
                  description:
                      'Toma una estructura JSON de pedido de la API de OrderFast y la convierte automáticamente a un ticket imprimible.',
                  mode: 'Gráfico (ViewPrintJob)',
                  onPressed: () => _invokePrint(
                    'printOrderTicket',
                    _cloverSdk.printOrderTicket(order: _sampleOrderForPrint),
                  ),
                ),
                _printCard(
                  title: '🖼️ 6. Impresión de Imagen PNG',
                  templateName: 'N/A (Motor ImagePrintJob2 de Clover)',
                  description:
                      'Imprime directamente gráficos, marcas de agua o imágenes codificadas en base64.',
                  mode: 'Imagen Bitmap',
                  onPressed: () => _invokePrint(
                    'printImage',
                    _cloverSdk.printImage(imageBase64: _kTinyPngBase64),
                  ),
                ),
              ]),
              _section('SDK', [
                _btn(
                    'initialize (reintentar)',
                    () => _invokeMap(
                          'initialize',
                          _cloverSdk.initialize(
                              remoteApplicationId: 'TU_RAID_AQUI'),
                        )),
              ]),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _cloverSdk.dispose();
    super.dispose();
  }
}
