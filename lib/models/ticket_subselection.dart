/// Modelo que representa una subselección de un item (modificador, opción adicional, etc.)
class TicketSubselection {
  /// Nombre de la subselección
  final String name;

  /// Cantidad de la subselección
  final int quantity;

  /// Precio unitario de la subselección
  final double price;

  /// Total de la subselección (precio * cantidad * cantidad del item padre)
  final double total;

  TicketSubselection({
    required this.name,
    required this.quantity,
    required this.price,
    required this.total,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'quantity': quantity,
      'price': price,
      'total': total,
    };
  }

  /// Crea un TicketSubselection desde un Map (acepta quantity como int o double desde JSON).
  factory TicketSubselection.fromMap(Map<String, dynamic> map) {
    final q = map['quantity'];
    final quantity = q is int ? q : (q is num ? q.toInt() : 0);
    return TicketSubselection(
      name: (map['name'] as String?) ?? '',
      quantity: quantity,
      price: (map['price'] as num?)?.toDouble() ?? 0.0,
      total: (map['total'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
