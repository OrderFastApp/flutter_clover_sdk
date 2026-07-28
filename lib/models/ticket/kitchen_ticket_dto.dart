import 'package:clover_sdk/models/ticket/ticket_item.dart';
import 'paper_profile.dart';

/// DTO para la generación de Comandas de Cocina (Alta Visibilidad)
class KitchenTicketDto {
  /// Número de pedido o turno (ej: "A73" o "Mesa 4")
  final String orderNumber;

  /// Lista de productos a preparar
  final List<TicketItem> items;

  /// Fecha y hora de la orden
  final String dateTime;

  /// Si es para llevar (true) o comer aquí (false)
  final bool? isTakeAway;

  /// Identificador opcional (Número de mesa, mozo, etc.)
  final String? identifier;

  /// Notas generales de la orden de cocina (ej: "Sin sal", "Entregar primero bebidas")
  final String? notes;

  /// Perfil de papel deseado (por defecto [PaperProfile.auto])
  final PaperProfile paperProfile;

  KitchenTicketDto({
    required this.orderNumber,
    required this.items,
    required this.dateTime,
    this.isTakeAway,
    this.identifier,
    this.notes,
    this.paperProfile = PaperProfile.auto,
  });

  Map<String, dynamic> toMap() {
    return {
      'orderNumber': orderNumber,
      'items': items.map((item) => item.toMap()).toList(),
      'dateTime': dateTime,
      if (isTakeAway != null) 'isTakeAway': isTakeAway,
      if (identifier != null && identifier!.isNotEmpty)
        'identifier': identifier,
      if (notes != null && notes!.isNotEmpty) 'notes': notes,
      'paperProfile': paperProfile.name,
    };
  }

  factory KitchenTicketDto.fromMap(Map<String, dynamic> map) {
    return KitchenTicketDto(
      orderNumber: map['orderNumber'] as String,
      items: (map['items'] as List<dynamic>)
          .map((item) => TicketItem.fromMap(item as Map<String, dynamic>))
          .toList(),
      dateTime: map['dateTime'] as String,
      isTakeAway: map['isTakeAway'] as bool?,
      identifier: map['identifier'] as String?,
      notes: map['notes'] as String?,
      paperProfile:
          PaperProfileExtension.fromString(map['paperProfile'] as String?),
    );
  }
}
