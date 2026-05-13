package ar.com.orderfast.printing

import ar.com.orderfast.models.NonFiscalTicket
import ar.com.orderfast.models.TicketItem

/**
 * Totales del pie del ticket no fiscal (Subtotal / Descuento / Total).
 *
 * Los montos del pie deben enviarse ya calculados desde la app:
 * [NonFiscalTicket.subtotalBeforeDiscount] y [NonFiscalTicket.discountAppliedAmount].
 * Si no se envían, solo se intenta un desglose cuando hay ítems con importe y metadatos de descuento
 * (código / porcentaje / monto informativo), usando la suma de líneas como subtotal.
 */
object NonFiscalReceiptTotals {

    private const val EPS = 0.005

    data class Resolved(
        val showBreakdown: Boolean,
        val subtotal: Double,
        val discount: Double,
        val total: Double,
    )

    fun sumLineTotals(items: List<TicketItem>): Double =
        items.asSequence().filter { it.quantity > 0 }.sumOf { it.total }

    /** Hay texto de descuento en el encabezado (código, % o monto informativo). */
    fun hasDiscountMeta(ticket: NonFiscalTicket): Boolean {
        val code = ticket.discountCode?.trim()?.isNotEmpty() == true
        return code || ticket.discountPercent != null || ticket.discountAmount != null
    }

    fun buildDiscountMetaLine(ticket: NonFiscalTicket, formatCurrency: (Double) -> String): String {
        val code = ticket.discountCode?.trim()?.takeIf { it.isNotEmpty() }
        val percent = ticket.discountPercent
        val amount = ticket.discountAmount
        return when {
            code != null && percent != null -> "Descuento: $code ${percent.toInt()}%"
            code != null && amount != null -> "Descuento: $code ${formatCurrency(amount)}"
            code != null -> "Descuento: $code"
            percent != null -> "Descuento: ${percent.toInt()}%"
            amount != null -> "Descuento: ${formatCurrency(amount)}"
            else -> ""
        }
    }

    /**
     * Resuelve subtotal y descuento del pie del recibo.
     * Prioridad: campos explícitos del ticket → suma de ítems + metadatos de descuento.
     */
    fun resolve(ticket: NonFiscalTicket): Resolved {
        val total = ticket.total
        val expSub = ticket.subtotalBeforeDiscount
        val expDisc = ticket.discountAppliedAmount

        if (expSub != null && expDisc != null) {
            return Resolved(
                showBreakdown = true,
                subtotal = expSub,
                discount = expDisc.coerceAtLeast(0.0),
                total = total,
            )
        }
        if (expSub != null) {
            val d = (expSub - total).coerceAtLeast(0.0)
            val show = d > EPS || expSub > total + EPS
            return Resolved(show, expSub, d, total)
        }
        if (expDisc != null) {
            val d = expDisc.coerceAtLeast(0.0)
            val s = total + d
            return Resolved(showBreakdown = d > EPS, subtotal = s, discount = d, total = total)
        }

        val sum = sumLineTotals(ticket.items)
        if (hasDiscountMeta(ticket) && sum > 0) {
            val d = (sum - total).coerceAtLeast(0.0)
            return Resolved(showBreakdown = d > EPS, subtotal = sum, discount = d, total = total)
        }
        return Resolved(showBreakdown = false, subtotal = total, discount = 0.0, total = total)
    }
}
