package ar.com.orderfast.printing.templates

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import ar.com.orderfast.models.ticket.KitchenTicketDto
import ar.com.orderfast.models.ticket.PaperProfile
import ar.com.orderfast.printing.styles.PrintTicketStyles

class KitchenTicketTemplate(private val context: Context) {

    fun buildView(dto: KitchenTicketDto, paperProfile: PaperProfile): View {
        val styles = PrintTicketStyles(context, paperProfile)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Encabezado estándar sin fondo negro
        layout.addView(styles.createHr("=", styles.lineCharCount))
        layout.addView(styles.createTextView("COMANDA DE COCINA #${dto.orderNumber}", bold = true, large = true, gravityVal = Gravity.CENTER))
        layout.addView(styles.createHr("-", styles.lineCharCount))

        if (dto.identifier != null && dto.identifier.isNotEmpty()) {
            layout.addView(styles.createTextView("Ubicación: ${dto.identifier}", bold = true, large = true, gravityVal = Gravity.CENTER))
        }

        // Modo de consumo (PARA LLEVAR / PARA COMER AQUI) en Fondo Negro Invertido
        if (dto.isTakeAway != null) {
            val modeText = if (dto.isTakeAway) "PARA LLEVAR" else "PARA COMER AQUI"
            layout.addView(styles.createInvertedBadge(modeText))
        }

        layout.addView(styles.createHr("=", styles.lineCharCount))

        // Lista de Ítems en texto grande estándar
        dto.items.forEach { item ->
            val itemLine = "${item.quantity}x ${item.description}"
            layout.addView(styles.createTextView(itemLine, bold = true, large = true))

            item.subselections?.forEach { sub ->
                layout.addView(styles.createTextView("   * ${sub.name}", bold = true, large = true))
            }

            if (!item.comment.isNullOrEmpty()) {
                layout.addView(styles.createTextView("   [NOTA: ${item.comment}]", bold = true, large = true))
            }
        }

        layout.addView(styles.createHr("=", styles.lineCharCount))

        // Notas generales de la orden
        if (!dto.notes.isNullOrEmpty()) {
            layout.addView(styles.createTextView("OBSERVACIONES:", bold = true, large = true))
            layout.addView(styles.createTextView(dto.notes, bold = true, large = true))
            layout.addView(styles.createHr("-", styles.lineCharCount))
        }

        layout.addView(styles.createTextView("Hora: ${dto.dateTime}", gravityVal = Gravity.CENTER))

        return layout
    }
}
