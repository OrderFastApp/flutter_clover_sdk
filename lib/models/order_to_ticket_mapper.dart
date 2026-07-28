import 'package:clover_sdk/models/ticket/non_fiscal_ticket.dart';
import 'package:clover_sdk/models/ticket/ticket_item.dart';
import 'package:clover_sdk/models/ticket/ticket_subselection.dart';

import 'order_ticket_models.dart';

/// Convierte una [TicketOrder] (JSON de la API) en [NonFiscalTicket] para imprimir.
NonFiscalTicket orderToNonFiscalTicket(
  TicketOrder order, {
  String? orderNumber,
  String? dateTime,
  bool? isTakeAway,
  String? identifier,
}) {
  final items = <TicketItem>[];

  for (final oi in order.orderItems) {
    if (oi.quantity <= 0) continue;

    final itemQty = oi.quantity;
    // Total del item = precio unitario * cantidad + suma de (sub.precio * sub.cantidad * cantidad del item)
    double subTotal = 0.0;
    final subselections = <TicketSubselection>[];

    for (final entry in oi.orderSubSelection) {
      final oss = entry.orderSubSelection;
      final qty = oss.quantity;
      if (qty <= 0 && oss.name.isEmpty) continue;
      final price = oss.price;
      // Total de esta subselección para la línea: price * qty * itemQty
      final lineTotal = price * qty * itemQty;
      subTotal += lineTotal;
      subselections.add(TicketSubselection(
        name: oss.name.isEmpty ? '-' : oss.name,
        quantity: qty.toInt(),
        price: price,
        total: lineTotal,
      ));
    }

    final unitPrice = oi.price;
    final itemLineTotal = (unitPrice * itemQty) + subTotal;

    items.add(TicketItem(
      quantity: itemQty.toInt(),
      description: oi.name,
      subtotal: unitPrice,
      total: itemLineTotal,
      comment: (oi.comment != null && oi.comment!.trim().isNotEmpty)
          ? oi.comment
          : null,
      subselections: subselections.isEmpty ? null : subselections,
    ));
  }

  String orderNum = orderNumber ?? order.id ?? '';
  if (orderNum.length > 12) {
    orderNum = orderNum.substring(0, 12);
  }

  String dt = dateTime ?? order.createdAt ?? '';
  if (dt.isNotEmpty && dt.length > 19) {
    dt = dt.substring(0, 19).replaceFirst('T', ' ');
  }

  String? discountCode;
  double? discountPercent;
  if (order.orderDiscounts != null) {
    discountCode = order.orderDiscounts!.discountCode;
    discountPercent = order.orderDiscounts!.discount;
    if (order.orderDiscounts!.discountType == 'percentage') {
      // ya está en porcentaje
    } else {
      discountPercent = null; // monto fijo: podríamos usar discountAmount
    }
  }

  final linesSum =
      items.fold<double>(0, (sum, i) => sum + (i.quantity > 0 ? i.total : 0));
  double? subtotalBeforeDiscount;
  double? discountAppliedAmount;
  if (order.orderDiscounts != null && linesSum > order.total + 0.0001) {
    subtotalBeforeDiscount = linesSum;
    discountAppliedAmount = linesSum - order.total;
  }

  return NonFiscalTicket(
    orderNumber: orderNum,
    items: items,
    total: order.total,
    dateTime: dt,
    isTakeAway: isTakeAway,
    identifier: identifier,
    discountCode: discountCode,
    discountPercent: discountPercent,
    discountAmount: order.orderDiscounts?.discountType != 'percentage'
        ? order.orderDiscounts?.discount
        : null,
    subtotalBeforeDiscount: subtotalBeforeDiscount,
    discountAppliedAmount: discountAppliedAmount,
  );
}
