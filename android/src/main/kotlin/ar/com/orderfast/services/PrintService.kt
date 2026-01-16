package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ar.com.orderfast.models.FiscalTicket
import ar.com.orderfast.models.NonFiscalTicket
import ar.com.orderfast.models.PrintTicketResponse
import ar.com.orderfast.models.TicketItem
import ar.com.orderfast.models.TicketSubselection
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.printer.job.ViewPrintJob

import android.os.Handler
import android.os.HandlerThread
import java.text.NumberFormat
import java.util.Locale

/**
 * Servicio para manejar la impresión de tickets con Clover
 */
class PrintService(private val context: Context) {

    companion object {
        private const val TAG = "PrintService"
        // Para papel de 80mm, el ancho típico es ~576 puntos (72 puntos por pulgada * 8 pulgadas)
        // Pero usaremos el ancho real de la impresora
        private const val DEFAULT_PRINTER_WIDTH = 576 // Ancho por defecto para papel de 80mm
        private const val LINE_WIDTH = DEFAULT_PRINTER_WIDTH
    }

    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    
    // HandlerThread para ejecutar operaciones de impresión en segundo plano
    private val printThread = HandlerThread("PrintThread").apply { start() }
    private val printHandler = Handler(printThread.looper)

    /**
     * Imprime un ticket no fiscal
     */
    fun printNonFiscalTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account: Account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(
                    success = false,
                    error = "No se pudo obtener la cuenta de Clover"
                ))
                return
            }

        // Ejecutar la creación del ViewPrintJob y la impresión en un hilo de fondo
        printHandler.post {
            try {
                // Usar ancho por defecto para papel de 80mm (576 puntos)
                // El ViewPrintJob ajustará automáticamente el layout
                val printerWidth = DEFAULT_PRINTER_WIDTH
                val view = createNonFiscalTicketView(ticket, printerWidth)
                
                // Medir y layout el View antes de pasarlo a ViewPrintJob
                val widthSpec = View.MeasureSpec.makeMeasureSpec(printerWidth, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                view.measure(widthSpec, heightSpec)
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                
                val printJob = ViewPrintJob.Builder()
                    .view(view, printerWidth)
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
    }

    /**
     * Imprime un ticket fiscal
     */
    fun printFiscalTicket(ticket: FiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account: Account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(
                    success = false,
                    error = "No se pudo obtener la cuenta de Clover"
                ))
                return
            }

        // Ejecutar la creación del ViewPrintJob y la impresión en un hilo de fondo
        printHandler.post {
            try {
                // Usar ancho por defecto para papel de 80mm (576 puntos)
                // El ViewPrintJob ajustará automáticamente el layout
                val printerWidth = DEFAULT_PRINTER_WIDTH
                val view = createFiscalTicketView(ticket, printerWidth)
                
                // Medir y layout el View antes de pasarlo a ViewPrintJob
                val widthSpec = View.MeasureSpec.makeMeasureSpec(printerWidth, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                view.measure(widthSpec, heightSpec)
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                
                val printJob = ViewPrintJob.Builder()
                    .view(view, printerWidth)
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
    }

    /**
     * Crea un View para el ticket no fiscal
     */
    private fun createNonFiscalTicketView(ticket: NonFiscalTicket, width: Int): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Separador superior
        layout.addView(createSeparatorLine(width, "="))

        // Número de pedido en negrita y mayúsculas
        layout.addView(createBoldTextView("TU NUMERO DE PEDIDO ES: ${ticket.orderNumber.uppercase()}"))
        layout.addView(createEmptyLine())

        // Preferencia (si aplica)
        if (ticket.isTakeAway != null) {
            val preferencia = if (ticket.isTakeAway == true) "PARA LLEVAR" else "PARA COMER AQUI"
            layout.addView(createBoldTextView("PREFERENCIA: $preferencia"))
            layout.addView(createEmptyLine())
        }

        // Identificador (si aplica y no es takeAway)
        if (ticket.isTakeAway != true && ticket.identifier != null && ticket.identifier!!.isNotEmpty()) {
            layout.addView(createBoldTextView("IDENTIFICADOR: ${ticket.identifier!!.uppercase()}"))
            layout.addView(createEmptyLine())
        }

        // Separadores antes de la tabla
        if (ticket.isTakeAway != null) {
            layout.addView(createEmptyLine())
            layout.addView(createSeparatorLine(width, "-"))
            layout.addView(createEmptyLine())
        }

        // Encabezado de la tabla
        layout.addView(createBoldTextView(buildTableHeader()))
        layout.addView(createSeparatorLine(width, "-"))

        // Items con subselecciones y comentarios
        ticket.items.forEach { item ->
            if (item.quantity > 0) {
                layout.addView(createBoldTextView(buildItemLine(item)))

                // Subselecciones
                if (!item.subselections.isNullOrEmpty()) {
                    item.subselections.forEach { subselection ->
                        layout.addView(createTextView(buildSubselectionLine(subselection, item.quantity)))
                    }
                    layout.addView(createEmptyLine())
                }

                // Comentario del item
                if (!item.comment.isNullOrEmpty() && item.comment!!.trim().isNotEmpty()) {
                    layout.addView(createBoldTextView("NOTA: ${item.comment!!.trim().uppercase()}"))
                }

                layout.addView(createEmptyLine())
            }
        }

        // Separador
        layout.addView(createSeparatorLine(width, "-"))

        // Total en negrita y tamaño grande
        layout.addView(createBoldLargeTextView(buildTotalLine(ticket.total)))
        layout.addView(createEmptyLine())

        // Separador
        layout.addView(createSeparatorLine(width, "="))
        layout.addView(createEmptyLine())

        // Fecha y hora centrada
        layout.addView(createCenteredTextView("----- ${ticket.dateTime} -----"))
        layout.addView(createEmptyLine())

        // Disclaimer centrado con asteriscos en negrita
        val disclaimer = ticket.disclaimer ?: "COMPROBANTE NO FISCAL, NO VALIDO COMO FACTURA."
        layout.addView(createCenteredBoldTextView("*************************"))
        layout.addView(createCenteredBoldTextView("COMPROBANTE NO FISCAL,"))
        layout.addView(createCenteredBoldTextView("NO VALIDO COMO FACTURA."))
        layout.addView(createCenteredBoldTextView("*************************"))

        // Espacio final
        layout.addView(createEmptyLine())
        layout.addView(createEmptyLine())

        return layout
    }

    /**
     * Crea un View para el ticket fiscal
     */
    private fun createFiscalTicketView(ticket: FiscalTicket, width: Int): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val fiscalInfo = ticket.fiscalInfo

        // Encabezado con tipo de comprobante
        val tipoComprobante = ticket.tipoComprobante ?: "RECIBO C"
        layout.addView(createCenteredBoldLargeTextView(tipoComprobante.uppercase()))
        layout.addView(createCenteredTextView("ORIGINAL Cod.: ${fiscalInfo.numeroT.takeIf { it.length <= 2 } ?: "15"}"))
        layout.addView(createSeparatorLine(width, "="))

        // Información del vendedor
        layout.addView(createTextView("RAZON SOCIAL: ${fiscalInfo.razonSocial.uppercase()}"))
        layout.addView(createTextView("CUIT: ${fiscalInfo.cuit}"))
        layout.addView(createTextView("DIRECCION: ${fiscalInfo.direccion.uppercase()}"))
        layout.addView(createTextView("LOCALIDAD: ${fiscalInfo.localidad.uppercase()}"))
        layout.addView(createTextView("Nro de insc. IIBB: ${fiscalInfo.numeroInscripcionIIBB}"))
        layout.addView(createTextView(fiscalInfo.responsable.uppercase()))
        layout.addView(createTextView("Inicio Actividades: ${fiscalInfo.inicioActividades}"))
        layout.addView(createSeparatorLine(width, "-"))

        // Detalles de la transacción
        layout.addView(createTextView("FECHA: ${fiscalInfo.fecha}"))
        layout.addView(createTextView("Nro T: ${fiscalInfo.numeroT}"))
        layout.addView(createTextView("Pto. Vta.:${fiscalInfo.puntoVenta}"))
        if (fiscalInfo.consumidorFinal) {
            layout.addView(createBoldTextView("CONSUMIDOR FINAL"))
        }
        layout.addView(createSeparatorLine(width, "-"))

        // Encabezado de la tabla
        layout.addView(createBoldTextView(buildTableHeader()))
        layout.addView(createSeparatorLine(width, "-"))

        // Items con subselecciones
        ticket.items.forEach { item ->
            if (item.quantity > 0) {
                layout.addView(createBoldTextView(buildItemLine(item)))

                // Subselecciones
                if (!item.subselections.isNullOrEmpty()) {
                    item.subselections.forEach { subselection ->
                        layout.addView(createTextView(buildSubselectionLine(subselection, item.quantity)))
                    }
                    layout.addView(createEmptyLine())
                }

                layout.addView(createEmptyLine())
            }
        }

        // Separador
        layout.addView(createSeparatorLine(width, "-"))

        // Total en negrita y tamaño grande
        layout.addView(createBoldLargeTextView(buildTotalLine(ticket.total)))
        layout.addView(createSeparatorLine(width, "-"))

        // Información fiscal
        if (fiscalInfo.regimenFiscal != null) {
            layout.addView(createTextView(fiscalInfo.regimenFiscal!!.uppercase()))
        }
        if (fiscalInfo.ivaContenido != null) {
            layout.addView(createTextView("IVA contenido: ${formatCurrency(fiscalInfo.ivaContenido)}"))
        }
        if (fiscalInfo.otrosImpuestosNacionales != null) {
            layout.addView(createTextView("Otro imp. Nacionales Indirectos: ${formatCurrency(fiscalInfo.otrosImpuestosNacionales)}"))
        }

        // CAE y fecha de vencimiento
        if (fiscalInfo.cae != null || fiscalInfo.fechaVencimiento != null) {
            layout.addView(createEmptyLine())
            val caeText = if (fiscalInfo.cae != null) "CAE:${fiscalInfo.cae}" else ""
            val fechaText = if (fiscalInfo.fechaVencimiento != null) "Fech Vto.: ${fiscalInfo.fechaVencimiento}" else ""
            if (caeText.isNotEmpty() && fechaText.isNotEmpty()) {
                val combined = "$caeText $fechaText"
                if (combined.length <= 48) {
                    layout.addView(createTextView(combined))
                } else {
                    if (caeText.isNotEmpty()) layout.addView(createTextView(caeText))
                    if (fechaText.isNotEmpty()) layout.addView(createTextView(fechaText))
                }
            } else {
                if (caeText.isNotEmpty()) layout.addView(createTextView(caeText))
                if (fechaText.isNotEmpty()) layout.addView(createTextView(fechaText))
            }
            layout.addView(createEmptyLine())
        }

        // Separador antes del QR
        layout.addView(createSeparatorLine(width, "="))
        layout.addView(createEmptyLine())

        // QR Code (nota: el QR code real se imprimiría como imagen, aquí solo indicamos)
        if (fiscalInfo.qrCodeData != null) {
            layout.addView(createCenteredTextView("[QR CODE]"))
            layout.addView(createEmptyLine())
        }

        // Separador después del QR
        layout.addView(createEmptyLine())
        layout.addView(createSeparatorLine(width, "-"))
        layout.addView(createEmptyLine())

        // Footer
        layout.addView(createCenteredTextView("Comprobable Autorizado"))
        layout.addView(createCenteredBoldTextView("Esta Administracion Federal no se responsabiliza por los datos"))
        layout.addView(createCenteredBoldTextView("ingresados en el detalle de la operacion"))

        // Espacio final
        layout.addView(createEmptyLine())
        layout.addView(createEmptyLine())

        return layout
    }

    /**
     * Crea un TextView normal
     */
    private fun createTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }

    /**
     * Crea un TextView en negrita
     */
    private fun createBoldTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            setTypeface(null, Typeface.BOLD)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }

    /**
     * Crea un TextView en negrita y tamaño grande
     */
    private fun createBoldLargeTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            setTypeface(null, Typeface.BOLD)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        }
    }

    /**
     * Crea un TextView centrado
     */
    private fun createCenteredTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }

    /**
     * Crea un TextView centrado y en negrita
     */
    private fun createCenteredBoldTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            setTypeface(null, Typeface.BOLD)
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }

    /**
     * Crea un TextView centrado, en negrita y tamaño grande
     */
    private fun createCenteredBoldLargeTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            setTypeface(null, Typeface.BOLD)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
    }

    /**
     * Crea una línea separadora
     */
    private fun createSeparatorLine(width: Int, character: String): TextView {
        // Para papel de 80mm, calcular aproximadamente cuántos caracteres caben
        // Asumiendo que cada carácter ocupa aproximadamente 6-8 puntos a tamaño de fuente 12
        val charCount = (width / 8).coerceAtLeast(1) // Mínimo 1 carácter
        val line = character.repeat(charCount)
        return TextView(context).apply {
            text = line
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }

    /**
     * Crea una línea vacía
     */
    private fun createEmptyLine(): TextView {
        return TextView(context).apply {
            text = " "
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 2, 0, 2)
            }
        }
    }

    /**
     * Construye las líneas del ticket no fiscal (deprecated - usar createNonFiscalTicketView)
     */
    @Deprecated("Usar createNonFiscalTicketView en su lugar")
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
