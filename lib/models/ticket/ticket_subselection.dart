/// Subselección de un ítem (modificador, ingrediente extra, opción, etc.)
class TicketSubselection {
  final String name;
  final int quantity;
  final double price;
  final double total;

  TicketSubselection({
    required this.name,
    required this.quantity,
    required this.price,
    double? total,
  }) : total = total ?? (price * quantity);

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'quantity': quantity,
      'price': price,
      'total': total,
    };
  }

  factory TicketSubselection.fromMap(Map<String, dynamic> map) {
    return TicketSubselection(
      name: map['name'] as String? ?? '',
      quantity: (map['quantity'] as num?)?.toInt() ?? 1,
      price: (map['price'] as num?)?.toDouble() ?? 0.0,
      total: (map['total'] as num?)?.toDouble(),
    );
  }
}
