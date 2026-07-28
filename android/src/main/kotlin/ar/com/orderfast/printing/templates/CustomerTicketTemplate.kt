package ar.com.orderfast.printing.templates

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import ar.com.orderfast.models.ticket.NonFiscalTicket
import ar.com.orderfast.models.ticket.PaperProfile
import ar.com.orderfast.printing.styles.PrintTicketStyles

class CustomerTicketTemplate(private val context: Context) {

    fun buildView(ticket: NonFiscalTicket, paperProfile: PaperProfile): View {
        val styles = PrintTicketStyles(context, paperProfile)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Encabezado
        layout.addView(styles.createHr("=", styles.lineCharCount))

        // Nombre de la Tienda (arriba, solo si existe)
        val nameToDisplay = ticket.storeName?.trim()
        if (!nameToDisplay.isNullOrEmpty()) {
            layout.addView(styles.createTextView(nameToDisplay.uppercase(), bold = true, large = true, gravityVal = Gravity.CENTER))
        }

        // Número de Orden en Badge de Fondo Negro
        layout.addView(styles.createInvertedBadge("ORDEN #${ticket.orderNumber}"))
        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Identificador (ej. Mesa 4) / Take Away
        if (ticket.identifier != null && ticket.identifier.isNotEmpty()) {
            layout.addView(styles.createTextView("Ubicación: ${ticket.identifier}", bold = true))
        }
        if (ticket.isTakeAway != null) {
            val modeText = if (ticket.isTakeAway) "PARA LLEVAR / DESTAPADO" else "PARA COMER AQUI"
            layout.addView(styles.createTextView(modeText, bold = true, gravityVal = Gravity.CENTER))
        }

        if (ticket.discountCode != null && ticket.discountCode.isNotEmpty()) {
            layout.addView(styles.createTextView("Desc. Código: ${ticket.discountCode}", bold = true))
        }

        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Encabezado Tabla
        layout.addView(styles.tableLine("CANT", "DESCRIPCION", "SUBT", "TOTAL", bold = true))
        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Productos
        ticket.items.forEach { item ->
            val subtotalStr = styles.formatCurrency(item.subtotal)
            val totalStr = styles.formatCurrency(item.total)
            layout.addView(styles.tableLine(
                qty = item.quantity.toString(),
                desc = item.description,
                subtotal = subtotalStr,
                total = totalStr,
                bold = true
            ))

            // Subselecciones
            item.subselections?.forEach { sub ->
                val subPriceStr = if (sub.price > 0) " (+${styles.formatCurrency(sub.price)})" else ""
                val subText = "  > ${sub.name}$subPriceStr"
                layout.addView(styles.createTextView(subText, small = true))
            }
        }

        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Pie de Totales
        if (ticket.subtotalBeforeDiscount != null) {
            val subStr = styles.formatCurrency(ticket.subtotalBeforeDiscount)
            layout.addView(styles.createTextView("Subtotal: $subStr", gravityVal = Gravity.END))
        }
        if (ticket.discountAppliedAmount != null && ticket.discountAppliedAmount > 0) {
            val discStr = styles.formatCurrency(ticket.discountAppliedAmount)
            layout.addView(styles.createTextView("Descuento: -$discStr", gravityVal = Gravity.END))
        }

        val finalTotalStr = styles.formatCurrency(ticket.total)
        layout.addView(styles.createTextView("TOTAL: $finalTotalStr", bold = true, large = true, gravityVal = Gravity.END))

        layout.addView(styles.createHr("=", styles.lineCharCount))

        val disclaimerText = ticket.disclaimer ?: "Comprobante no válido como factura"
        if (disclaimerText.isNotEmpty()) {
            layout.addView(styles.createTextView(disclaimerText, bold = true, small = true, gravityVal = Gravity.CENTER))
        }

        layout.addView(styles.createTextView("----- ${ticket.dateTime} -----", gravityVal = Gravity.CENTER, small = true))

        return layout
    }
}
