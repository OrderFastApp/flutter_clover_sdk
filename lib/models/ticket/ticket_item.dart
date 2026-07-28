import 'ticket_subselection.dart';

/// Ítem individual dentro de un ticket
class TicketItem {
  final int quantity;
  final String description;
  final double subtotal;
  final double total;
  final String? comment;
  final List<TicketSubselection>? subselections;

  TicketItem({
    required this.quantity,
    required this.description,
    required this.subtotal,
    required this.total,
    this.comment,
    this.subselections,
  });

  Map<String, dynamic> toMap() {
    return {
      'quantity': quantity,
      'description': description,
      'subtotal': subtotal,
      'total': total,
      if (comment != null && comment!.isNotEmpty) 'comment': comment,
      if (subselections != null)
        'subselections': subselections!.map((sub) => sub.toMap()).toList(),
    };
  }

  factory TicketItem.fromMap(Map<String, dynamic> map) {
    return TicketItem(
      quantity: (map['quantity'] as num?)?.toInt() ?? 1,
      description: map['description'] as String? ?? '',
      subtotal: (map['subtotal'] as num?)?.toDouble() ?? 0.0,
      total: (map['total'] as num?)?.toDouble() ?? 0.0,
      comment: map['comment'] as String?,
      subselections: map['subselections'] != null
          ? (map['subselections'] as List<dynamic>)
              .map((sub) => TicketSubselection.fromMap(sub as Map<String, dynamic>))
              .toList()
          : null,
    );
  }
}
