import 'ticket_item.dart';
import 'fiscal_info.dart';

/// Modelo que representa un ticket fiscal
class FiscalTicket {
  /// Número de pedido (opcional)
  final String? orderNumber;

  /// Lista de items del ticket
  final List<TicketItem> items;

  /// Total del ticket
  final double total;

  /// Fecha y hora de la transacción
  final String dateTime;

  /// Información fiscal del ticket
  final FiscalInfo fiscalInfo;

  /// Tipo de comprobante (ej: "Recibo C", "Factura A", etc.)
  final String? tipoComprobante;

  FiscalTicket({
    this.orderNumber,
    required this.items,
    required this.total,
    required this.dateTime,
    required this.fiscalInfo,
    this.tipoComprobante,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      if (orderNumber != null) 'orderNumber': orderNumber,
      'items': items.map((item) => item.toMap()).toList(),
      'total': total,
      'dateTime': dateTime,
      'fiscalInfo': fiscalInfo.toMap(),
      if (tipoComprobante != null) 'tipoComprobante': tipoComprobante,
    };
  }

  /// Crea un FiscalTicket desde un Map
  factory FiscalTicket.fromMap(Map<String, dynamic> map) {
    return FiscalTicket(
      orderNumber: map['orderNumber'] as String?,
      items: (map['items'] as List<dynamic>)
          .map((item) => TicketItem.fromMap(item as Map<String, dynamic>))
          .toList(),
      total: (map['total'] as num).toDouble(),
      dateTime: map['dateTime'] as String,
      fiscalInfo: FiscalInfo.fromMap(
        map['fiscalInfo'] as Map<String, dynamic>,
      ),
      tipoComprobante: map['tipoComprobante'] as String?,
    );
  }
}
