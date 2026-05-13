import 'package:flutter/services.dart';

/// Canal nativo `clover_sdk`.
///
/// Argumentos frecuentes: `nonFiscalTicket` (mapa con `orderNumber`, `items`, `total`,
/// `dateTime`, y opcional `subtotalBeforeDiscount`, `discountAppliedAmount`, descuentos meta, etc.).
const MethodChannel methodChannel = MethodChannel('clover_sdk');
