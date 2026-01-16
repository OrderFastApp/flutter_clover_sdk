package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.util.Log
import ar.com.orderfast.models.FiscalTicket
import ar.com.orderfast.models.NonFiscalTicket
import ar.com.orderfast.models.PrintTicketResponse
import ar.com.orderfast.models.TicketItem
import ar.com.orderfast.models.TicketSubselection
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.printer.job.TextPrintJob
import java.text.NumberFormat
import java.util.Locale

/**
 * Servicio para manejar la impresión de tickets con Clover
 */
class PrintService(private val context: Context) {

    companion object {
        private const val TAG = "PrintService"
        private const val LINE_WIDTH = 48 // Ancho típico de una impresora de tickets (48 caracteres)
    }

    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    /**
     * Imprime un ticket no fiscal
     */
    fun printNonFiscalTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        try {
            val account: Account = CloverAccount.getAccount(context)
                ?: run {
                    callback(PrintTicketResponse(
                        success = false,
                        error = "No se pudo obtener la cuenta de Clover"
                    ))
                    return
                }

            val lines = buildNonFiscalTicketLines(ticket)
            val printJob = TextPrintJob.Builder()
                .lines(lines)
                .build()

            printJob.print(context, account)

            Log.d(TAG, "Ticket no fiscal enviado a impresión: ${ticket.orderNumber}")
            callback(PrintTicketResponse(
                success = true,
                message = "Ticket impreso correctamente"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir ticket no fiscal", e)
            callback(PrintTicketResponse(
                success = false,
                error = e.message ?: "Error desconocido al imprimir"
            ))
        }
    }

    /**
     * Imprime un ticket fiscal
     */
    fun printFiscalTicket(ticket: FiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        try {
            val account: Account = CloverAccount.getAccount(context)
                ?: run {
                    callback(PrintTicketResponse(
                        success = false,
                        error = "No se pudo obtener la cuenta de Clover"
                    ))
                    return
                }

            val lines = buildFiscalTicketLines(ticket)
            val printJob = TextPrintJob.Builder()
                .lines(lines)
                .build()

            printJob.print(context, account)

            Log.d(TAG, "Ticket fiscal enviado a impresión")
            callback(PrintTicketResponse(
                success = true,
                message = "Ticket fiscal impreso correctamente"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir ticket fiscal", e)
            callback(PrintTicketResponse(
                success = false,
                error = e.message ?: "Error desconocido al imprimir"
            ))
        }
    }

    /**
     * Construye las líneas del ticket no fiscal
     */
    private fun buildNonFiscalTicketLines(ticket: NonFiscalTicket): List<String> {
        val lines = mutableListOf<String>()

        // Separador superior
        lines.add("=".repeat(LINE_WIDTH))

        // Número de pedido (formato similar al ejemplo)
        lines.add("TU NUMERO DE PEDIDO ES: ${ticket.orderNumber}")
        lines.add("")

        // Preferencia (si aplica)
        if (ticket.isTakeAway != null) {
            val preferencia = if (ticket.isTakeAway == true) "Para llevar" else "Para comer aquí"
            lines.add("Preferencia: $preferencia")
            lines.add("")
        }

        // Identificador (si aplica y no es takeAway)
        if (ticket.isTakeAway != true && ticket.identifier != null && ticket.identifier!!.isNotEmpty()) {
            lines.add("Identificador: ${ticket.identifier}")
            lines.add("")
        }

        // Separadores antes de la tabla
        if (ticket.isTakeAway != null) {
            lines.add(" ")
            lines.add("-".repeat(LINE_WIDTH))
            lines.add(" ")
        }

        // Encabezado de la tabla
        lines.add(buildTableHeader())
        lines.add("-".repeat(LINE_WIDTH))

        // Items con subselecciones y comentarios
        ticket.items.forEach { item ->
            if (item.quantity > 0) {
                lines.add(buildItemLine(item))

                // Subselecciones
                if (!item.subselections.isNullOrEmpty()) {
                    item.subselections.forEach { subselection ->
                        lines.add(buildSubselectionLine(subselection, item.quantity))
                    }
                    lines.add("")
                }

                // Comentario del item
                if (!item.comment.isNullOrEmpty() && item.comment!!.trim().isNotEmpty()) {
                    lines.add("NOTA: ${item.comment!!.trim()}")
                }

                lines.add(" ")
            }
        }

        // Separador
        lines.add("-".repeat(LINE_WIDTH))

        // Total
        lines.add(buildTotalLine(ticket.total))
        lines.add("")

        // Separador
        lines.add("=".repeat(LINE_WIDTH))
        lines.add("")

        // Fecha y hora centrada
        lines.add(centerText("----- ${ticket.dateTime} -----"))
        lines.add("")

        // Disclaimer centrado con asteriscos
        val disclaimer = ticket.disclaimer ?: "COMPROBANTE NO FISCAL, NO VALIDO COMO FACTURA."
        lines.add(centerText("*************************"))
        lines.add(centerText("COMPROBANTE NO FISCAL,"))
        lines.add(centerText("NO VALIDO COMO FACTURA."))
        lines.add(centerText("*************************"))

        // Espacio final
        lines.add("")
        lines.add("")

        return lines
    }

    /**
     * Construye las líneas del ticket fiscal
     */
    private fun buildFiscalTicketLines(ticket: FiscalTicket): List<String> {
        val lines = mutableListOf<String>()

        val fiscalInfo = ticket.fiscalInfo

        // Encabezado con tipo de comprobante
        val tipoComprobante = ticket.tipoComprobante ?: "Recibo C"
        lines.add(centerText(tipoComprobante))
        lines.add("ORIGINAL Cod.: ${fiscalInfo.numeroT.takeIf { it.length <= 2 } ?: "15"}")
        lines.add("=".repeat(LINE_WIDTH))

        // Información del vendedor
        lines.add("RAZON SOCIAL: ${fiscalInfo.razonSocial}")
        lines.add("CUIT: ${fiscalInfo.cuit}")
        lines.add("DIRECCION: ${fiscalInfo.direccion}")
        lines.add("LOCALIDAD: ${fiscalInfo.localidad}")
        lines.add("Nro de insc. IIBB: ${fiscalInfo.numeroInscripcionIIBB}")
        lines.add(fiscalInfo.responsable)
        lines.add("Inicio Actividades: ${fiscalInfo.inicioActividades}")
        lines.add("-".repeat(LINE_WIDTH))

        // Detalles de la transacción
        lines.add("FECHA: ${fiscalInfo.fecha}")
        lines.add("Nro T: ${fiscalInfo.numeroT}")
        lines.add("Pto. Vta.:${fiscalInfo.puntoVenta}")
        if (fiscalInfo.consumidorFinal) {
            lines.add("CONSUMIDOR FINAL")
        }
        lines.add("-".repeat(LINE_WIDTH))

        // Encabezado de la tabla
        lines.add(buildTableHeader())
        lines.add("-".repeat(LINE_WIDTH))

        // Items con subselecciones
        ticket.items.forEach { item ->
            if (item.quantity > 0) {
                lines.add(buildItemLine(item))

                // Subselecciones
                if (!item.subselections.isNullOrEmpty()) {
                    item.subselections.forEach { subselection ->
                        lines.add(buildSubselectionLine(subselection, item.quantity))
                    }
                    lines.add("")
                }

                lines.add(" ")
            }
        }

        // Separador
        lines.add("-".repeat(LINE_WIDTH))

        // Total
        lines.add(buildTotalLine(ticket.total))
        lines.add("-".repeat(LINE_WIDTH))

        // Información fiscal
        if (fiscalInfo.regimenFiscal != null) {
            lines.add(fiscalInfo.regimenFiscal)
        }
        if (fiscalInfo.ivaContenido != null) {
            lines.add("IVA contenido: ${formatCurrency(fiscalInfo.ivaContenido)}")
        }
        if (fiscalInfo.otrosImpuestosNacionales != null) {
            lines.add("Otro imp. Nacionales Indirectos: ${formatCurrency(fiscalInfo.otrosImpuestosNacionales)}")
        }

        // CAE y fecha de vencimiento en la misma línea
        if (fiscalInfo.cae != null || fiscalInfo.fechaVencimiento != null) {
            lines.add(" ")
            val caeText = if (fiscalInfo.cae != null) "CAE:${fiscalInfo.cae}" else ""
            val fechaText = if (fiscalInfo.fechaVencimiento != null) "Fech Vto.: ${fiscalInfo.fechaVencimiento}" else ""
            if (caeText.isNotEmpty() && fechaText.isNotEmpty()) {
                // Intentar ponerlos en la misma línea si caben
                val combined = "$caeText $fechaText"
                if (combined.length <= LINE_WIDTH) {
                    lines.add(combined)
                } else {
                    lines.add(caeText)
                    lines.add(fechaText)
                }
            } else {
                if (caeText.isNotEmpty()) lines.add(caeText)
                if (fechaText.isNotEmpty()) lines.add(fechaText)
            }
            lines.add(" ")
        }

        // Separador antes del QR
        lines.add("=".repeat(LINE_WIDTH))
        lines.add("")

        // QR Code (nota: el QR code real se imprimiría como imagen, aquí solo indicamos)
        if (fiscalInfo.qrCodeData != null) {
            lines.add("[QR CODE]")
            lines.add("")
        }

        // Separador después del QR
        lines.add(" ")
        lines.add("-".repeat(LINE_WIDTH))
        lines.add("")

        // Footer
        lines.add(centerText("Comprobable Autorizado"))
        lines.add(centerText("Esta Administracion Federal no se responsabiliza por los datos"))
        lines.add(centerText("ingresados en el detalle de la operacion"))

        // Espacio final
        lines.add("")
        lines.add("")

        return lines
    }

    /**
     * Construye el encabezado de la tabla
     */
    private fun buildTableHeader(): String {
        return String.format(
            "%-5s %-25s %-8s %-8s",
            "Cant.",
            "Descripcion",
            "SubTot.",
            "Total"
        ).trim()
    }

    /**
     * Construye una línea de item
     */
    private fun buildItemLine(item: TicketItem): String {
        val maxDescLength = 25
        val desc = if (item.description.length > maxDescLength) {
            item.description.substring(0, maxDescLength - 3) + "..."
        } else {
            item.description
        }

        // Formato: Cantidad | Descripción | Subtotal | Total
        return String.format(
            "%-5d %-25s %8s %8s",
            item.quantity,
            desc,
            formatCurrency(item.subtotal),
            formatCurrency(item.total)
        ).trim()
    }

    /**
     * Construye una línea de subselección
     */
    private fun buildSubselectionLine(subselection: TicketSubselection, parentQuantity: Int): String {
        val maxDescLength = 25
        val desc = if (subselection.name.length > maxDescLength) {
            subselection.name.substring(0, maxDescLength - 3) + "..."
        } else {
            subselection.name
        }

        // Formato: espacio | "nombre x cantidad" | precio unitario | total
        val subDesc = "$desc x ${subselection.quantity}"
        return String.format(
            "     %-25s %8s %8s",
            subDesc,
            formatCurrency(subselection.price),
            formatCurrency(subselection.total)
        ).trim()
    }

    /**
     * Construye la línea del total
     */
    private fun buildTotalLine(total: Double): String {
        // Formatear como moneda y remover el símbolo $ duplicado si existe
        val formatted = formatCurrency(total)
        val totalText = if (formatted.startsWith("$")) formatted else "\$$formatted"
        return String.format(
            "%-35s %13s",
            "Total:",
            totalText
        ).trim()
    }

    /**
     * Formatea un número como moneda
     */
    private fun formatCurrency(amount: Double): String {
        return numberFormat.format(amount)
    }

    /**
     * Centra un texto en la línea
     */
    private fun centerText(text: String): String {
        // Si el texto es más largo que el ancho de línea, simplemente retornarlo sin centrar
        if (text.length >= LINE_WIDTH) {
            return text
        }
        val padding = (LINE_WIDTH - text.length) / 2
        // Asegurarse de que padding nunca sea negativo
        val safePadding = maxOf(0, padding)
        return " ".repeat(safePadding) + text
    }
}
