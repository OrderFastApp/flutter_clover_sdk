import 'ticket_subselection.dart';

/// Modelo que representa un item de un ticket
class TicketItem {
  /// Cantidad del item
  final int quantity;

  /// Descripción del item
  final String description;

  /// Subtotal del item (precio unitario)
  final double subtotal;

  /// Total del item (precio unitario * cantidad)
  final double total;

  /// Comentario o nota del item
  final String? comment;

  /// Subselecciones del item (opciones adicionales, modificadores, etc.)
  final List<TicketSubselection>? subselections;

  TicketItem({
    required this.quantity,
    required this.description,
    required this.subtotal,
    required this.total,
    this.comment,
    this.subselections,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      'quantity': quantity,
      'description': description,
      'subtotal': subtotal,
      'total': total,
      if (comment != null && comment!.isNotEmpty) 'comment': comment,
      if (subselections != null && subselections!.isNotEmpty)
        'subselections': subselections!.map((s) => s.toMap()).toList(),
    };
  }

  /// Crea un TicketItem desde un Map (acepta quantity como int o double desde JSON).
  factory TicketItem.fromMap(Map<String, dynamic> map) {
    final q = map['quantity'];
    final quantity = q is int ? q : (q is num ? q.toInt() : 0);
    return TicketItem(
      quantity: quantity,
      description: (map['description'] as String?) ?? '',
      subtotal: (map['subtotal'] as num?)?.toDouble() ?? 0.0,
      total: (map['total'] as num?)?.toDouble() ?? 0.0,
      comment: map['comment'] as String?,
      subselections: map['subselections'] != null
          ? (map['subselections'] as List<dynamic>)
              .map((s) => TicketSubselection.fromMap(s as Map<String, dynamic>))
              .toList()
          : null,
    );
  }
}
