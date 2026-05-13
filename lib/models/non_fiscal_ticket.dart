import 'ticket_item.dart';

/// Modelo que representa un ticket no fiscal
class NonFiscalTicket {
  /// Número de pedido
  final String orderNumber;

  /// Lista de items del ticket
  final List<TicketItem> items;

  /// Total del ticket (monto final cobrado)
  final double total;

  /// Fecha y hora de la transacción
  final String dateTime;

  /// Si es para llevar (true) o para comer aquí (false). Null si no aplica
  final bool? isTakeAway;

  /// Identificador de la orden (mesa, etc.)
  final String? identifier;

  /// Texto de disclaimer (por defecto: "COMPROBANTE NO FISCAL, NO VALIDO COMO FACTURA.")
  final String? disclaimer;

  /// Código o etiqueta del descuento (ej: "descuentito"). Opcional; línea informativa arriba del ticket.
  final String? discountCode;

  /// Porcentaje de descuento (ej: 50 para 50%). Opcional; línea informativa.
  final double? discountPercent;

  /// Monto informativo del descuento para el encabezado (texto "Descuento: …"). No reemplaza al pie numérico.
  final double? discountAmount;

  /// Subtotal antes del descuento para el **pie** del ticket (Subtotal / Descuento / Total). Enviar ya calculado.
  final double? subtotalBeforeDiscount;

  /// Monto del descuento aplicado (positivo) para el pie ("Descuento: -$…"). Enviar ya calculado.
  final double? discountAppliedAmount;

  NonFiscalTicket({
    required this.orderNumber,
    required this.items,
    required this.total,
    required this.dateTime,
    this.isTakeAway,
    this.identifier,
    this.disclaimer,
    this.discountCode,
    this.discountPercent,
    this.discountAmount,
    this.subtotalBeforeDiscount,
    this.discountAppliedAmount,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      'orderNumber': orderNumber,
      'items': items.map((item) => item.toMap()).toList(),
      'total': total,
      'dateTime': dateTime,
      if (isTakeAway != null) 'isTakeAway': isTakeAway,
      if (identifier != null && identifier!.isNotEmpty) 'identifier': identifier,
      if (disclaimer != null) 'disclaimer': disclaimer,
      if (discountCode != null && discountCode!.isNotEmpty) 'discountCode': discountCode,
      if (discountPercent != null) 'discountPercent': discountPercent,
      if (discountAmount != null) 'discountAmount': discountAmount,
      if (subtotalBeforeDiscount != null) 'subtotalBeforeDiscount': subtotalBeforeDiscount,
      if (discountAppliedAmount != null) 'discountAppliedAmount': discountAppliedAmount,
    };
  }

  /// Crea un NonFiscalTicket desde un Map
  factory NonFiscalTicket.fromMap(Map<String, dynamic> map) {
    return NonFiscalTicket(
      orderNumber: map['orderNumber'] as String,
      items: (map['items'] as List<dynamic>)
          .map((item) => TicketItem.fromMap(item as Map<String, dynamic>))
          .toList(),
      total: (map['total'] as num).toDouble(),
      dateTime: map['dateTime'] as String,
      isTakeAway: map['isTakeAway'] as bool?,
      identifier: map['identifier'] as String?,
      disclaimer: map['disclaimer'] as String?,
      discountCode: map['discountCode'] as String?,
      discountPercent: map['discountPercent'] != null
          ? (map['discountPercent'] as num).toDouble()
          : null,
      discountAmount: map['discountAmount'] != null
          ? (map['discountAmount'] as num).toDouble()
          : null,
      subtotalBeforeDiscount: map['subtotalBeforeDiscount'] != null
          ? (map['subtotalBeforeDiscount'] as num).toDouble()
          : null,
      discountAppliedAmount: map['discountAppliedAmount'] != null
          ? (map['discountAppliedAmount'] as num).toDouble()
          : null,
    );
  }
}
