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
  String _lastResponse = '';

  @override
  void initState() {
    super.initState();
    _setupCallbacks();
  }

  void _setupCallbacks() {
    _cloverSdk.onInitialized = (response) {
      setState(() {
        _status = response['success'] == true
            ? 'Inicializado correctamente'
            : 'Error al inicializar';
        _lastResponse = response.toString();
      });
    };

    _cloverSdk.onServiceDisconnected = (response) {
      setState(() {
        _status = 'Servicio desconectado';
        _lastResponse = response.toString();
      });
    };

    _cloverSdk.onSaleResponse = (response) {
      setState(() {
        _status = 'Respuesta de venta recibida';
        _lastResponse = response.toString();
      });
    };

    _cloverSdk.onRefundPaymentResponse = (response) {
      setState(() {
        _status = 'Respuesta de reembolso recibida';
        _lastResponse = response.toString();
      });
    };

    _cloverSdk.onConfirmPaymentRequest = (request) {
      setState(() {
        _status = 'Confirmación de pago requerida';
        _lastResponse = request.toString();
      });
      // Aquí puedes confirmar o rechazar el pago
      // _cloverSdk.confirmPayment(payment: request['payment'], challenges: request['challenges']);
    };
  }

  Future<void> _initialize() async {
    final result = await _cloverSdk.initialize();
    setState(() {
      _status = result['success'] == true
          ? 'Inicializando...'
          : 'Error: ${result['error']}';
    });
  }

  Future<void> _sale() async {
    final result = await _cloverSdk.sale(
      amount: 1000, // $10.00 en centavos
      externalId: 'order_${DateTime.now().millisecondsSinceEpoch}',
    );
    setState(() {
      _status = result['success'] == true
          ? 'Venta enviada'
          : 'Error: ${result['error']}';
    });
  }

  Future<void> _disconnect() async {
    final result = await _cloverSdk.disconnect();
    setState(() {
      _status = result['success'] == true
          ? 'Desconectado'
          : 'Error: ${result['error']}';
    });
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
                      Text(
                        'Estado: $_status',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      if (_lastResponse.isNotEmpty)
                        Text(
                          'Última respuesta:',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      if (_lastResponse.isNotEmpty)
                        Text(
                          _lastResponse,
                          style: const TextStyle(fontSize: 12),
                        ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _initialize,
                child: const Text('Inicializar SDK'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _sale,
                child: const Text('Realizar Venta ($10.00)'),
              ),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _disconnect,
                child: const Text('Desconectar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
