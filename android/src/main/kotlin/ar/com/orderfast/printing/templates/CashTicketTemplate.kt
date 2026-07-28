package ar.com.orderfast.printing.templates

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import ar.com.orderfast.models.ticket.NonFiscalTicket
import ar.com.orderfast.models.ticket.PaperProfile
import ar.com.orderfast.printing.styles.PrintTicketStyles

class CashTicketTemplate(private val context: Context) {

    fun buildView(ticket: NonFiscalTicket, paperProfile: PaperProfile): View {
        val styles = PrintTicketStyles(context, paperProfile)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(styles.createHr("=", styles.lineCharCount))
        layout.addView(styles.createTextView("COMPROBANTE DE CAJA", bold = true, large = true, gravityVal = Gravity.CENTER))
        layout.addView(styles.createTextView("ORDEN #${ticket.orderNumber}", bold = true, gravityVal = Gravity.CENTER))
        layout.addView(styles.createHr("-", styles.lineCharCount))

        if (ticket.items.isNotEmpty()) {
            ticket.items.forEach { item ->
                val priceText = styles.formatCurrency(item.total)
                layout.addView(styles.tableLine(
                    qty = item.quantity.toString(),
                    desc = item.description,
                    subtotal = "",
                    total = priceText,
                    bold = false
                ))
            }
            layout.addView(styles.createHr("-", styles.lineCharCount))
        }

        val totalStr = styles.formatCurrency(ticket.total)
        layout.addView(styles.createTextView("TOTAL CAJA: $totalStr", bold = true, large = true, gravityVal = Gravity.END))

        layout.addView(styles.createHr("=", styles.lineCharCount))
        layout.addView(styles.createTextView("Fecha: ${ticket.dateTime}", gravityVal = Gravity.CENTER, small = true))

        return layout
    }
}
