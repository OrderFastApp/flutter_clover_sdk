import 'paper_profile.dart';
import 'ticket_item.dart';

/// Modelo que representa un ticket no fiscal (Comprobante de Venta / Cliente / Caja)
class NonFiscalTicket {
  /// Número de pedido
  final String orderNumber;

  /// Lista de items del ticket
  final List<TicketItem> items;

  /// Total del ticket (monto final cobrado)
  final double total;

  /// Fecha y hora de la transacción
  final String dateTime;

  /// Nombre de la tienda / comercio (opcional). Se muestra arriba del número de orden.
  final String? storeName;

  /// Si es para llevar (true) o para comer aquí (false). Null si no aplica
  final bool? isTakeAway;

  /// Identificador de la orden (mesa, etc.)
  final String? identifier;

  /// Texto de disclaimer (por defecto: "COMPROBANTE NO FISCAL, NO VALIDO COMO FACTURA.")
  final String? disclaimer;

  /// Código o etiqueta del descuento (ej: "descuentito"). Opcional.
  final String? discountCode;

  /// Porcentaje de descuento (ej: 50 para 50%). Opcional.
  final double? discountPercent;

  /// Monto informativo del descuento para el encabezado.
  final double? discountAmount;

  /// Subtotal antes del descuento para el pie del ticket.
  final double? subtotalBeforeDiscount;

  /// Monto del descuento aplicado (positivo) para el pie.
  final double? discountAppliedAmount;

  /// Perfil de ancho de papel deseado (por defecto [PaperProfile.auto] para detección física de hardware)
  final PaperProfile paperProfile;

  NonFiscalTicket({
    required this.orderNumber,
    required this.items,
    required this.total,
    required this.dateTime,
    this.storeName,
    this.isTakeAway,
    this.identifier,
    this.disclaimer,
    this.discountCode,
    this.discountPercent,
    this.discountAmount,
    this.subtotalBeforeDiscount,
    this.discountAppliedAmount,
    this.paperProfile = PaperProfile.auto,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      'orderNumber': orderNumber,
      'items': items.map((item) => item.toMap()).toList(),
      'total': total,
      'dateTime': dateTime,
      if (storeName != null && storeName!.trim().isNotEmpty) 'storeName': storeName!.trim(),
      if (isTakeAway != null) 'isTakeAway': isTakeAway,
      if (identifier != null && identifier!.isNotEmpty) 'identifier': identifier,
      if (disclaimer != null) 'disclaimer': disclaimer,
      if (discountCode != null && discountCode!.isNotEmpty) 'discountCode': discountCode,
      if (discountPercent != null) 'discountPercent': discountPercent,
      if (discountAmount != null) 'discountAmount': discountAmount,
      if (subtotalBeforeDiscount != null) 'subtotalBeforeDiscount': subtotalBeforeDiscount,
      if (discountAppliedAmount != null) 'discountAppliedAmount': discountAppliedAmount,
      'paperProfile': paperProfile.name,
    };
  }

  /// Crea un NonFiscalTicket desde un Map con parseo flexible de storeName
  factory NonFiscalTicket.fromMap(Map<String, dynamic> map) {
    final rawStoreName = _parseStoreName(map);

    return NonFiscalTicket(
      orderNumber: map['orderNumber'] as String? ?? '',
      items: (map['items'] as List<dynamic>?)
              ?.map((item) => TicketItem.fromMap(item as Map<String, dynamic>))
              .toList() ??
          [],
      total: (map['total'] as num?)?.toDouble() ?? 0.0,
      dateTime: map['dateTime'] as String? ?? '',
      storeName: rawStoreName,
      isTakeAway: map['isTakeAway'] as bool?,
      identifier: map['identifier'] as String?,
      disclaimer: map['disclaimer'] as String?,
      discountCode: map['discountCode'] as String?,
      discountPercent: (map['discountPercent'] as num?)?.toDouble(),
      discountAmount: (map['discountAmount'] as num?)?.toDouble(),
      subtotalBeforeDiscount: (map['subtotalBeforeDiscount'] as num?)?.toDouble(),
      discountAppliedAmount: (map['discountAppliedAmount'] as num?)?.toDouble(),
      paperProfile: PaperProfileExtension.fromString(map['paperProfile'] as String?),
    );
  }

  static String? _parseStoreName(Map<String, dynamic> map) {
    if (map['storeName'] != null) return map['storeName'].toString();
    if (map['store_name'] != null) return map['store_name'].toString();
    if (map['store'] != null) {
      final store = map['store'];
      if (store is Map && store['name'] != null) return store['name'].toString();
      if (store is String && store.isNotEmpty) return store;
    }
    if (map['name'] != null) return map['name'].toString();
    if (map['slug'] != null) return map['slug'].toString();
    if (map['store_slug'] != null) return map['store_slug'].toString();
    return null;
  }
}
