import 'ticket_item.dart';

/// Modelo que representa un ticket no fiscal
class NonFiscalTicket {
  /// Número de pedido
  final String orderNumber;

  /// Lista de items del ticket
  final List<TicketItem> items;

  /// Total del ticket
  final double total;

  /// Fecha y hora de la transacción
  final String dateTime;

  /// Si es para llevar (true) o para comer aquí (false). Null si no aplica
  final bool? isTakeAway;

  /// Identificador de la orden (mesa, etc.)
  final String? identifier;

  /// Texto de disclaimer (por defecto: "COMPROBANTE NO FISCAL, NO VALIDO COMO FACTURA.")
  final String? disclaimer;

  NonFiscalTicket({
    required this.orderNumber,
    required this.items,
    required this.total,
    required this.dateTime,
    this.isTakeAway,
    this.identifier,
    this.disclaimer,
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
    );
  }
}
