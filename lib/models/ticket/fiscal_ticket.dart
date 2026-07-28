import 'fiscal_info.dart';
import 'paper_profile.dart';
import 'ticket_item.dart';

/// Modelo que representa un ticket fiscal (AFIP)
class FiscalTicket {
  /// Número de orden/pedido (opcional)
  final String? orderNumber;

  /// Lista de items del ticket
  final List<TicketItem> items;

  /// Total del ticket (monto final cobrado)
  final double total;

  /// Fecha y hora de la transacción
  final String dateTime;

  /// Información fiscal del emisor/receptor
  final FiscalInfo fiscalInfo;

  /// Tipo de comprobante (ej: "Recibo C", "Factura B"). Opcional.
  final String? tipoComprobante;

  /// Perfil de ancho de papel deseado (por defecto [PaperProfile.auto])
  final PaperProfile paperProfile;

  FiscalTicket({
    required this.items,
    required this.total,
    required this.dateTime,
    required this.fiscalInfo,
    this.orderNumber,
    this.tipoComprobante,
    this.paperProfile = PaperProfile.auto,
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
      'paperProfile': paperProfile.name,
    };
  }

  /// Crea un FiscalTicket desde un Map
  factory FiscalTicket.fromMap(Map<String, dynamic> map) {
    return FiscalTicket(
      orderNumber: map['orderNumber'] as String?,
      items: (map['items'] as List<dynamic>?)
              ?.map((item) => TicketItem.fromMap(item as Map<String, dynamic>))
              .toList() ??
          [],
      total: (map['total'] as num?)?.toDouble() ?? 0.0,
      dateTime: map['dateTime'] as String? ?? '',
      fiscalInfo: FiscalInfo.fromMap(map['fiscalInfo'] as Map<String, dynamic>),
      tipoComprobante: map['tipoComprobante'] as String?,
      paperProfile: PaperProfileExtension.fromString(map['paperProfile'] as String?),
    );
  }
}
