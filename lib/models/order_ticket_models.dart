// Modelos que representan el JSON de una orden para imprimir tickets.
// Compatibles con la estructura de la API (order_items, order_sub_selection, order_discounts).

// Objeto interno que define la subselección elegida (el que importa para el ticket).
class OrderSubSelectionData {
  final String? productId;
  final String? subselectionId;
  final String? orderId;
  final String? createdAt;
  final double quantity;
  final double price;
  final String name;

  OrderSubSelectionData({
    this.productId,
    this.subselectionId,
    this.orderId,
    this.createdAt,
    required this.quantity,
    required this.price,
    required this.name,
  });

  factory OrderSubSelectionData.fromJson(Map<String, dynamic> json) {
    return OrderSubSelectionData(
      productId: json['product_id'] as String?,
      subselectionId: json['subselection_id'] as String?,
      orderId: json['order_id'] as String?,
      createdAt: json['created_at'] as String?,
      quantity: _toDouble(json['quantity']),
      price: _toDouble(json['price']),
      name: (json['name'] as String?) ?? '',
    );
  }
}

/// Un elemento del array order_sub_selection (contiene order_sub_selection y subselection).
class TicketOrderSubSelectionEntry {
  final OrderSubSelectionData orderSubSelection;
  final Map<String, dynamic>? subselection;
  final dynamic subselectionItems;

  TicketOrderSubSelectionEntry({
    required this.orderSubSelection,
    this.subselection,
    this.subselectionItems,
  });

  factory TicketOrderSubSelectionEntry.fromJson(Map<String, dynamic> json) {
    final oss = json['order_sub_selection'];
    return TicketOrderSubSelectionEntry(
      orderSubSelection: oss != null
          ? OrderSubSelectionData.fromJson(
              oss is Map<String, dynamic> ? oss : Map<String, dynamic>.from(oss as Map))
          : OrderSubSelectionData(quantity: 0, price: 0, name: ''),
      subselection: json['subselection'] != null
          ? Map<String, dynamic>.from(json['subselection'] as Map)
          : null,
      subselectionItems: json['subselection_items'],
    );
  }
}

/// Item de la orden (order_items[]).
class TicketOrderItem {
  final String? id;
  final String? createdAt;
  final String? productId;
  final double quantity;
  final String? comment;
  final String? orderId;
  final List<TicketOrderSubSelectionEntry> orderSubSelection;
  final String name;
  final double price;

  TicketOrderItem({
    this.id,
    this.createdAt,
    this.productId,
    required this.quantity,
    this.comment,
    this.orderId,
    required this.orderSubSelection,
    required this.name,
    required this.price,
  });

  factory TicketOrderItem.fromJson(Map<String, dynamic> json) {
    final rawSub = json['order_sub_selection'];
    List<TicketOrderSubSelectionEntry> subs = [];
    if (rawSub is List) {
      for (final e in rawSub) {
        if (e is Map<String, dynamic>) {
          subs.add(TicketOrderSubSelectionEntry.fromJson(e));
        } else if (e is Map) {
          subs.add(TicketOrderSubSelectionEntry.fromJson(Map<String, dynamic>.from(e)));
        }
      }
    }
    return TicketOrderItem(
      id: json['id'] as String?,
      createdAt: json['created_at'] as String?,
      productId: json['product_id'] as String?,
      quantity: _toDouble(json['quantity']),
      comment: json['comment'] as String?,
      orderId: json['order_id'] as String?,
      orderSubSelection: subs,
      name: (json['name'] as String?) ?? '',
      price: _toDouble(json['price']),
    );
  }
}

/// Descuento de la orden (opcional).
class OrderDiscounts {
  final String? locationId;
  final String? id;
  final String? createdAt;
  final String? discountCode;
  final String? creatorId;
  final String? expireAt;
  final String? startAt;
  final double discount;
  final String? discountType;

  OrderDiscounts({
    this.locationId,
    this.id,
    this.createdAt,
    this.discountCode,
    this.creatorId,
    this.expireAt,
    this.startAt,
    required this.discount,
    this.discountType,
  });

  factory OrderDiscounts.fromJson(Map<String, dynamic> json) {
    return OrderDiscounts(
      locationId: json['location_id'] as String?,
      id: json['id'] as String?,
      createdAt: json['created_at'] as String?,
      discountCode: json['discount_code'] as String?,
      creatorId: json['creator_id'] as String?,
      expireAt: json['expire_at'] as String?,
      startAt: json['start_at'] as String?,
      discount: _toDouble(json['discount']),
      discountType: json['discount_type'] as String?,
    );
  }
}

/// Orden completa para ticket (raíz del JSON).
class TicketOrder {
  final String? locationId;
  final String? totemId;
  final String? sortId;
  final String? id;
  final String? createdAt;
  final String? updatedAt;
  final double total;
  final String? comments;
  final String? status;
  final String? paymentId;
  final String? language;
  final List<TicketOrderItem> orderItems;
  final OrderDiscounts? orderDiscounts;
  final Map<String, dynamic>? paymentMethod;
  final dynamic orderPayment;
  final dynamic location;
  final dynamic totem;
  final String? storeName;

  TicketOrder({
    this.locationId,
    this.totemId,
    this.sortId,
    this.id,
    this.createdAt,
    this.updatedAt,
    required this.total,
    this.comments,
    this.status,
    this.paymentId,
    this.language,
    required this.orderItems,
    this.orderDiscounts,
    this.paymentMethod,
    this.orderPayment,
    this.location,
    this.totem,
    this.storeName,
  });

  factory TicketOrder.fromJson(Map<String, dynamic> json) {
    final rawItems = json['order_items'];
    List<TicketOrderItem> items = [];
    if (rawItems is List) {
      for (final e in rawItems) {
        if (e is Map<String, dynamic>) {
          items.add(TicketOrderItem.fromJson(e));
        } else if (e is Map) {
          items.add(TicketOrderItem.fromJson(Map<String, dynamic>.from(e)));
        }
      }
    }
    final rawDiscounts = json['order_discounts'];

    String? extractedStoreName;
    if (json['storeName'] != null) {
      extractedStoreName = json['storeName'].toString();
    } else if (json['store_name'] != null) {
      extractedStoreName = json['store_name'].toString();
    } else if (json['store'] != null) {
      final store = json['store'];
      if (store is Map && store['name'] != null) {
        extractedStoreName = store['name'].toString();
      } else if (store is String && store.isNotEmpty) {
        extractedStoreName = store;
      }
    } else if (json['name'] != null) {
      extractedStoreName = json['name'].toString();
    } else if (json['slug'] != null) {
      extractedStoreName = json['slug'].toString();
    } else if (json['store_slug'] != null) {
      extractedStoreName = json['store_slug'].toString();
    }

    return TicketOrder(
      locationId: json['location_id'] as String?,
      totemId: json['totem_id'] as String?,
      sortId: json['sort_id'] as String?,
      id: json['id'] as String?,
      createdAt: json['created_at'] as String?,
      updatedAt: json['updated_at'] as String?,
      total: _toDouble(json['total']),
      comments: json['comments'] as String?,
      status: json['status'] as String?,
      paymentId: json['payment_id'] as String?,
      language: json['language'] as String?,
      orderItems: items,
      orderDiscounts: rawDiscounts != null
          ? OrderDiscounts.fromJson(rawDiscounts is Map<String, dynamic>
              ? rawDiscounts
              : Map<String, dynamic>.from(rawDiscounts as Map))
          : null,
      paymentMethod: json['payment_method'] != null
          ? Map<String, dynamic>.from(json['payment_method'] as Map)
          : null,
      orderPayment: json['order_payment'],
      location: json['location'],
      totem: json['totem'],
      storeName: extractedStoreName,
    );
  }
}

double _toDouble(dynamic value) {
  if (value == null) return 0.0;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}
