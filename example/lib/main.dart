import 'package:flutter/material.dart';
import 'package:clover_sdk/clover_sdk.dart';

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
          _status = 'Pago Exitoso';
          _lastPayment = response['payment'];
        } else {
          _status = 'Error: ${response['reason']} - ${response['message']}';
        }
      });
    };
  }

  Future<void> _initializeSDK() async {
    // IMPORTANTE: Reemplaza 'TU_RAID_AQUI' con tu Remote Application ID
    // Obtén tu RAID desde: https://www.clover.com/developers
    final result = await _cloverSdk.initialize(
      remoteApplicationId: 'TU_RAID_AQUI', // Reemplaza con tu RAID
    );
    
    if (result['success'] != true) {
      setState(() {
        _status = 'Error al inicializar: ${result['error']}';
      });
      return;
    }
    
    // Mantener la pantalla encendida (útil para aplicaciones POS)
    await _cloverSdk.keepScreenOn(keepOn: true);
    
    // Activar modo inmersivo para ocultar barras del sistema (opcional)
    await _cloverSdk.setImmersiveMode(
      hideStatusBar: true,
      hideNavigationBar: true,
    );
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
      _status = 'Procesando pago de \$${amount.toStringAsFixed(2)}...';
    });

    await _cloverSdk.sale(
      amount: amountInCents,
      externalId: externalId,
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Clover SDK Example'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
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
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (_lastPayment != null) ...[
                        const SizedBox(height: 16),
                        const Divider(),
                        const SizedBox(height: 8),
                        const Text(
                          'Último Pago:',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text('ID: ${_lastPayment!['id']}'),
                        Text('Monto: \$${(_lastPayment!['amount']! / 100).toStringAsFixed(2)}'),
                        if (_lastPayment!['tipAmount'] != null)
                          Text('Propina: \$${(_lastPayment!['tipAmount']! / 100).toStringAsFixed(2)}'),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _deviceConnected ? () => _processPayment(10.00) : null,
                child: const Text('Pagar \$10.00'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _deviceConnected ? () => _processPayment(25.50) : null,
                child: const Text('Pagar \$25.50'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _deviceConnected ? () => _processPayment(50.00) : null,
                child: const Text('Pagar \$50.00'),
              ),
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
