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

  /// Crea un TicketSubselection desde un Map
  factory TicketSubselection.fromMap(Map<String, dynamic> map) {
    return TicketSubselection(
      name: map['name'] as String,
      quantity: map['quantity'] as int,
      price: (map['price'] as num).toDouble(),
      total: (map['total'] as num).toDouble(),
    );
  }
}
