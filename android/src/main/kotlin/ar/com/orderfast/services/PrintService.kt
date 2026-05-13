package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.util.Base64
import android.util.Log
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.Layout
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import ar.com.orderfast.printing.NonFiscalReceiptTotals
import ar.com.orderfast.models.FiscalTicket
import ar.com.orderfast.models.NonFiscalTicket
import ar.com.orderfast.models.PrintTicketResponse
import ar.com.orderfast.models.TicketItem
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.printer.job.ImagePrintJob2
import com.clover.sdk.v1.printer.job.ViewPrintJob
import com.clover.sdk.v3.merchant.MerchantDevicesV2Connector

import android.os.Handler
import android.os.HandlerThread
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import ar.com.orderfast.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
/**
 * Servicio para manejar la impresión de tickets con Clover
 */
class PrintService(private val context: Context) {

    companion object {
        private const val TAG = "PrintService"
        private const val PRINTER_WIDTH_80MM = 576
        private const val PRINTER_WIDTH_55MM = 384
        private const val LINE_CHAR_80MM = 22
        private const val LINE_CHAR_55MM = 14
        /** Separadores cortos como en el ticket (líneas de guiones o iguales). */
        private const val SHORT_SEP_LENGTH = 25
        /** Línea de iguales corta en el encabezado (varias líneas de ~6 caracteres). */
        private const val TOP_EQ_LENGTH = 20
        /** Asteriscos que enmarcan el disclaimer. */
        private const val ASTERISK_LENGTH = 20
        /** Altura máxima en píxeles para ImagePrintJob2 (Clover SDK). */
        private const val IMAGE_MAX_HEIGHT = 2048
    }

    private enum class PaperProfile {
        MM55,
        MM80
    }

    private val currentPaperProfile: PaperProfile by lazy { detectPaperProfile() }
    private val printerWidthPx: Int
        get() = if (currentPaperProfile == PaperProfile.MM55) PRINTER_WIDTH_55MM else PRINTER_WIDTH_80MM
    private val lineCharCount: Int
        get() = if (currentPaperProfile == PaperProfile.MM55) LINE_CHAR_55MM else LINE_CHAR_80MM
    private val is55mmPaper: Boolean
        get() = currentPaperProfile == PaperProfile.MM55

    /** Anchos fijos en caracteres monoespaciados (CANT + DESCR + SUBTOT + TOTAL). */
    private data class TicketColumns(
        val qty: Int,
        val desc: Int,
        val subtotal: Int,
        val total: Int,
    ) {
        val widthChars: Int get() = qty + desc + subtotal + total
    }

    private val columns: TicketColumns
        get() = if (is55mmPaper) {
            TicketColumns(qty = 3, desc = 13, subtotal = 9, total = 9)
        } else {
            TicketColumns(qty = 5, desc = 22, subtotal = 12, total = 12)
        }

    private fun fixedCell(value: String, width: Int, alignEnd: Boolean = false): String {
        val clean = value.replace("\n", " ").trim()
        val truncated = if (clean.length > width) {
            clean.take((width - 1).coerceAtLeast(1)) + "…"
        } else {
            clean
        }
        return if (alignEnd) {
            truncated.padStart(width)
        } else {
            truncated.padEnd(width)
        }
    }

    private fun tableLine(
        qty: String,
        desc: String,
        subtotal: String,
        total: String,
        bold: Boolean = false,
    ): TextView {
        val c = columns
        val line =
            fixedCell(qty, c.qty) +
                fixedCell(desc, c.desc) +
                fixedCell(subtotal, c.subtotal, alignEnd = true) +
                fixedCell(total, c.total, alignEnd = true)
        return TextView(context).apply {
            text = line
            typeface = if (bold) robotoMonoBold else robotoMono
            textSize = if (is55mmPaper) 14f else 17f
            includeFontPadding = false
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun tableSeparatorLine(): TextView {
        return TextView(context).apply {
            text = "-".repeat(columns.widthChars)
            typeface = robotoMono
            textSize = if (is55mmPaper) 14f else 17f
            includeFontPadding = false
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    /** Roboto Mono para tickets (monoespaciada, ideal para recibos). Fallback a MONOSPACE si falla la carga. */
    private val robotoMono: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.roboto_mono) ?: Typeface.MONOSPACE
    }

    /** Roboto Mono en negrita (synthetic bold sobre la misma fuente). */
    private val robotoMonoBold: Typeface by lazy {
        Typeface.create(robotoMono, Typeface.BOLD)
    }

    /** Símbolos numéricos argentinos: punto para miles, coma decimal. */
    private val argSymbols: DecimalFormatSymbols by lazy {
        DecimalFormatSymbols(Locale("es", "AR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
    }

    /** Formato argentino con dos decimales: $ 4.900,00. */
    private val numberFormat: NumberFormat by lazy { DecimalFormat("#,##0.00", argSymbols) }

    /** Formato argentino sin decimales: $ 4.900. */
    private val integerNumberFormat: NumberFormat by lazy { DecimalFormat("#,##0", argSymbols) }

    /**
     * Moneda con espacio detrás del `$`. Si el monto no tiene centavos (ej. 6930,00) se imprime
     * sin la parte decimal para reducir el ancho y evitar saltos de línea raros en pantalla angosta.
     */
    private fun formatCurrency(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        val formatter = if (cents % 100L == 0L) integerNumberFormat else numberFormat
        return "\$ ${formatter.format(amount)}"
    }

    private fun detectPaperProfile(): PaperProfile {
        return try {
            val device = MerchantDevicesV2Connector(context).getDevice()
            val rawDeviceInfo = listOfNotNull(
                device?.deviceTypeName,
                device?.productName,
                device?.model
            ).joinToString(" ").lowercase(Locale.US)

            when {
                rawDeviceInfo.contains("mini") || rawDeviceInfo.contains("flex") -> PaperProfile.MM55
                else -> PaperProfile.MM80
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo detectar el modelo Clover, se usa 80mm por defecto", e)
            PaperProfile.MM80
        }
    }

    // HandlerThread para ejecutar operaciones de impresión en segundo plano
    private val printThread = HandlerThread("PrintThread").apply { start() }
    private val printHandler = Handler(printThread.looper)

    /**
     * Construye el [View], mide, hace layout e imprime con [ViewPrintJob] en el hilo de impresión.
     */
    /**
     * Libera bitmaps asignados a [ImageView] (p. ej. QR fiscal) tras el [ViewPrintJob].
     */
    private fun recycleImageViewBitmaps(root: View) {
        when (root) {
            is ImageView -> {
                val bmp = (root.drawable as? BitmapDrawable)?.bitmap
                root.setImageDrawable(null)
                if (bmp != null && !bmp.isRecycled) {
                    bmp.recycle()
                }
            }
            is ViewGroup -> {
                for (i in 0 until root.childCount) {
                    recycleImageViewBitmaps(root.getChildAt(i))
                }
            }
        }
    }

    private fun printView(
        account: Account,
        buildView: () -> View,
        successMessage: String,
        logMessage: String,
        callback: (PrintTicketResponse) -> Unit,
    ) {
        printHandler.post {
            var rootView: View? = null
            try {
                val printerWidth = printerWidthPx
                val view = buildView()
                rootView = view
                val widthSpec = View.MeasureSpec.makeMeasureSpec(printerWidth, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                view.measure(widthSpec, heightSpec)
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                ViewPrintJob.Builder()
                    .view(view, printerWidth)
                    .build()
                    .print(context, account)

                Log.d(TAG, logMessage)
                callback(PrintTicketResponse(success = true, message = successMessage))
            } catch (e: Exception) {
                Log.e(TAG, "Error al imprimir", e)
                callback(
                    PrintTicketResponse(
                        success = false,
                        error = e.message ?: "Error desconocido al imprimir",
                    ),
                )
            } finally {
                rootView?.let { recycleImageViewBitmaps(it) }
            }
        }
    }

    /**
     * Imprime un ticket no fiscal
     */
    fun printNonFiscalTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(
                    PrintTicketResponse(
                        success = false,
                        error = "No se pudo obtener la cuenta de Clover",
                    ),
                )
                return
            }
        printView(
            account = account,
            buildView = { createNonFiscalTicketView(ticket, printerWidthPx) },
            successMessage = "Ticket impreso correctamente",
            logMessage = "Ticket no fiscal enviado a impresión: ${ticket.orderNumber}",
            callback = callback,
        )
    }

    /**
     * Imprime un ticket de caja (cash ticket) con formato abreviado.
     * Recibe el mismo modelo de datos que el ticket no fiscal.
     */
    fun printCashTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(
                    PrintTicketResponse(
                        success = false,
                        error = "No se pudo obtener la cuenta de Clover",
                    ),
                )
                return
            }
        printView(
            account = account,
            buildView = { createCashTicketView(ticket, printerWidthPx) },
            successMessage = "Cash ticket impreso correctamente",
            logMessage = "Cash ticket enviado a impresión: ${ticket.orderNumber}",
            callback = callback,
        )
    }

    /**
     * Imprime un ticket fiscal
     */
    fun printFiscalTicket(ticket: FiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(
                    PrintTicketResponse(
                        success = false,
                        error = "No se pudo obtener la cuenta de Clover",
                    ),
                )
                return
            }
        printView(
            account = account,
            buildView = { createFiscalTicketView(ticket, printerWidthPx) },
            successMessage = "Ticket fiscal impreso correctamente",
            logMessage = "Ticket fiscal enviado a impresión",
            callback = callback,
        )
    }

    /**
     * Imprime una imagen desde base64 usando ImagePrintJob2 (Clover SDK).
     * El bitmap se escala automáticamente al ancho del papel.
     * Altura máxima: 2048px (si excede se reescala).
     *
     * @param base64 Imagen en base64 (puede incluir prefijo data:image/...;base64,)
     */
    fun printImage(base64: String, callback: (PrintTicketResponse) -> Unit) {
        val account: Account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(
                    success = false,
                    error = "No se pudo obtener la cuenta de Clover"
                ))
                return
            }

        // build() hace IO bloqueante, debe ejecutarse en hilo de fondo
        printHandler.post {
            try {
                val cleanBase64 = base64.substringAfter(",").trim().ifEmpty { base64 }
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: run {
                        callback(PrintTicketResponse(
                            success = false,
                            error = "No se pudo decodificar la imagen base64"
                        ))
                        return@post
                    }

                val scaledBitmap = if (bitmap.height > IMAGE_MAX_HEIGHT) {
                    val scale = IMAGE_MAX_HEIGHT.toFloat() / bitmap.height
                    val newWidth = (bitmap.width * scale).toInt()
                    Bitmap.createScaledBitmap(bitmap, newWidth, IMAGE_MAX_HEIGHT, true).also {
                        if (it != bitmap) bitmap.recycle()
                    }
                } else {
                    bitmap
                }

                val printJob = ImagePrintJob2.Builder(context)
                    .bitmap(scaledBitmap)
                    .build()

                printJob.print(context, account, null)

                if (scaledBitmap != bitmap) scaledBitmap.recycle()

                Log.d(TAG, "Imagen enviada a impresión")
                callback(PrintTicketResponse(
                    success = true,
                    message = "Imagen impresa correctamente"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Error al imprimir imagen", e)
                callback(PrintTicketResponse(
                    success = false,
                    error = e.message ?: "Error desconocido al imprimir imagen"
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

        // Referencia: hr('=')
        layout.addView(createSeparatorLine("=", TOP_EQ_LENGTH))
        layout.addView(createOrdenHeaderRow(ticket.orderNumber))
        layout.addView(createSeparatorLine("-", lineCharCount))

        addNonFiscalMetaBlock(layout, ticket)

        if (ticket.isTakeAway != null) {
            addTripleSpaceDashSpace(layout)
        }

        layout.addView(createProductLines(ticket.items))

        layout.addView(createSeparatorLine("-", lineCharCount))

        addTotalsBlock(layout, ticket)

        layout.addView(createSeparatorLine("=", TOP_EQ_LENGTH))
        layout.addView(createEmptyLine())

        layout.addView(createCenteredTextView("----- ${ticket.dateTime} -----"))
        layout.addView(createEmptyLine())

        layout.addView(createCenteredBoldTextView("*".repeat(ASTERISK_LENGTH)))
        layout.addView(createCenteredBoldTextView("COMPROBANTE NO FISCAL,"))
        layout.addView(createCenteredBoldTextView("NO VALIDO COMO FACTURA."))
        layout.addView(createCenteredBoldTextView("*".repeat(ASTERISK_LENGTH)))

        layout.addView(createEmptyLine())
        layout.addView(createEmptyLine())

        return layout
    }

    /**
     * Crea un View para ticket de caja con formato resumido.
     */
    private fun createCashTicketView(ticket: NonFiscalTicket, width: Int): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Referencia `printOrder`: =, ORDEN fila, hr, callout, =, meta sin detalle, hr, totales, =, fecha, disclaimer
        layout.addView(createSeparatorLine("=", TOP_EQ_LENGTH))
        layout.addView(createOrdenHeaderRow(ticket.orderNumber))
        layout.addView(createSeparatorLine("-", lineCharCount))

        if (is55mmPaper) {
            layout.addView(createCenteredBoldTextView("POR FAVOR ABONAR"))
            layout.addView(createCenteredBoldTextView("EN CAJA"))
        } else {
            layout.addView(createCenteredBoldLargeTextView("POR FAVOR ABONAR EN CAJA"))
        }
        layout.addView(createEmptyLine())

        layout.addView(createSeparatorLine("=", TOP_EQ_LENGTH))

        addNonFiscalMetaBlock(layout, ticket)

        layout.addView(createSeparatorLine("-", lineCharCount))

        addTotalsBlock(layout, ticket)

        layout.addView(createSeparatorLine("=", TOP_EQ_LENGTH))
        layout.addView(createEmptyLine())

        layout.addView(createCenteredTextView("----- ${ticket.dateTime} -----"))
        layout.addView(createEmptyLine())

        layout.addView(createCenteredBoldTextView("*".repeat(ASTERISK_LENGTH)))
        layout.addView(createCenteredBoldTextView("COMPROBANTE NO FISCAL,"))
        layout.addView(createCenteredBoldTextView("NO VALIDO COMO FACTURA."))
        layout.addView(createCenteredBoldTextView("*".repeat(ASTERISK_LENGTH)))
        layout.addView(createEmptyLine())
        layout.addView(createEmptyLine())

        return layout
    }

    /**
     * Líneas de productos como [TextView] monoespaciados (sin [TableLayout]).
     */
    private fun createProductLines(items: List<TicketItem>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setPadding(0, 0, 0, 4)

            addView(tableLine("Cant.", "Descripcion", "SubTot.", "Total", bold = true))
            addView(tableSeparatorLine())

            items.forEach { item ->
                if (item.quantity <= 0) return@forEach

                val productName = item.description.trim()
                addView(
                    tableLine(
                        qty = item.quantity.toString(),
                        desc = productName,
                        subtotal = formatCurrency(item.subtotal),
                        total = formatCurrency(item.total),
                        bold = true,
                    ),
                )

                if (!item.subselections.isNullOrEmpty()) {
                    item.subselections.forEach { sub ->
                        val subName = sub.name.trim()
                        val subLabel = "$subName x ${sub.quantity}"
                        addView(
                            tableLine(
                                qty = "",
                                desc = " $subLabel",
                                subtotal = formatCurrency(sub.price),
                                total = formatCurrency(sub.total),
                                bold = false,
                            ),
                        )
                    }
                    addView(createEmptyLine())
                }

                item.comment?.trim()?.takeIf { it.isNotEmpty() }?.let { note ->
                    addView(createMetaBoldLine("NOTA: $note"))
                }

                addView(createSeparatorLine(" ", lineCharCount))
            }
        }
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
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))

        // Información del vendedor
        layout.addView(createTextView("RAZON SOCIAL: ${fiscalInfo.razonSocial.uppercase()}"))
        layout.addView(createTextView("CUIT: ${fiscalInfo.cuit}"))
        layout.addView(createTextView("DIRECCION: ${fiscalInfo.direccion.uppercase()}"))
        layout.addView(createTextView("LOCALIDAD: ${fiscalInfo.localidad.uppercase()}"))
        layout.addView(createTextView("Nro de insc. IIBB: ${fiscalInfo.numeroInscripcionIIBB}"))
        layout.addView(createTextView(fiscalInfo.responsable.uppercase()))
        layout.addView(createTextView("Inicio Actividades: ${fiscalInfo.inicioActividades}"))
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))

        // Detalles de la transacción
        layout.addView(createTextView("FECHA: ${fiscalInfo.fecha}"))
        layout.addView(createTextView("Nro T: ${fiscalInfo.numeroT}"))
        layout.addView(createTextView("Pto. Vta.:${fiscalInfo.puntoVenta}"))
        if (fiscalInfo.consumidorFinal) {
            layout.addView(createBoldTextView("CONSUMIDOR FINAL"))
        }
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))

        layout.addView(createProductLines(ticket.items))

        // Separador
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))

        // Total pegado a la derecha
        layout.addView(createTotalLineView(ticket.total))
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))

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
            if (caeText.isNotEmpty()) layout.addView(createTextView(caeText))
            if (fechaText.isNotEmpty()) layout.addView(createTextView(fechaText))
            layout.addView(createEmptyLine())
        }

        // Separador antes del QR

        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))
        layout.addView(createEmptyLine())

        // QR Code (nota: el QR code real se imprimiría como imagen, aquí solo indicamos)
        if (fiscalInfo.qrCodeData != null) {
            fiscalInfo.qrCodeData
                ?.takeIf { it.isNotBlank() }
                ?.let { qrData ->
                    layout.addView(createQrImageView(qrData, size = 260))
                    layout.addView(createEmptyLine())
                }
        }

        // Separador después del QR
        layout.addView(createEmptyLine())
        layout.addView(createSeparatorLine("-", SHORT_SEP_LENGTH))
        layout.addView(createEmptyLine())

        return layout
    }

    /**
     * Crea un TextView normal
     */
    private fun createTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMono
            textSize = if (is55mmPaper) 20f else 27f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
            }
        }
    }

    private fun createQrBitmap(data: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints,
        )
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private fun createQrImageView(data: String, size: Int = 260): ImageView {
        val bitmap = createQrBitmap(data, size)
        return ImageView(context).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 8
                bottomMargin = 8
            }
        }
    }

    /**
     * Crea un TextView en negrita
     */
    private fun createBoldTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 20f else 27f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
            }
        }
    }

    /**
     * Crea un TextView en negrita y tamaño grande
     */
    private fun createBoldLargeTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 27f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 3 else 6, 0, if (is55mmPaper) 3 else 6)
            }
        }
    }

    /**
     * Crea un TextView centrado
     */
    private fun createCenteredTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMono
            textSize = if (is55mmPaper) 20f else 27f
            includeFontPadding = false
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
            }
        }
    }

    /**
     * Crea un TextView centrado y en negrita
     */
    private fun createCenteredBoldTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 19f else 24f
            includeFontPadding = false
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
            }
        }
    }

    /**
     * Crea un TextView centrado, en negrita y tamaño grande
     */
    private fun createCenteredBoldLargeTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text.uppercase()
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 27f
            includeFontPadding = false
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 4 else 8, 0, if (is55mmPaper) 4 else 8)
            }
        }
    }

    /**
     * Crea una línea separadora.
     * @param character Carácter a repetir (= o -)
     * @param length Si no es null, número de caracteres (línea corta). Si es null, usa fullWidth.
     * @param fullWidth Ancho en puntos para línea larga cuando length == null.
     */
    private fun createSeparatorLine(character: String, length: Int? = null, fullWidth: Int? = null): TextView {
        val effectiveWidth = fullWidth ?: printerWidthPx
        val charCount = length ?: (effectiveWidth / 8).coerceAtLeast(1)
        val line = character.repeat(charCount)
        return TextView(context).apply {
            text = line
            typeface = robotoMono
            textSize = if (is55mmPaper) 20f else 27f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
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
                setMargins(0, if (is55mmPaper) 0 else 2, 0, if (is55mmPaper) 0 else 2)
            }
        }
    }

    /**
     * Vista de la línea del total: "TOTAL:" pegado a la izquierda, monto pegado a la derecha.
     */
    private fun createTotalLineView(total: Double): View {
        val totalText = formatCurrency(total)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 3 else 6, 0, if (is55mmPaper) 3 else 6)
            }
        }
        // TOTAL: pegado a la izquierda
        row.addView(TextView(context).apply {
            text = "TOTAL:"
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 30f
            includeFontPadding = false
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        // Monto pegado a la derecha (ocupa el resto del ancho y se alinea a la derecha)
        row.addView(TextView(context).apply {
            text = totalText
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 30f
            includeFontPadding = false
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })
        return row
    }

    /**
     * Fila `ORDEN:` + número (referencia PosColumn 6+6).
     */
    private fun createOrdenHeaderRow(orderNumber: String): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, if (is55mmPaper) 3 else 6, 0, if (is55mmPaper) 3 else 6)
            }
        }

        row.addView(TextView(context).apply {
            text = "ORDEN:"
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 30f
            includeFontPadding = false
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })

        row.addView(TextView(context).apply {
            text = orderNumber.trim()
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 22f else 30f
            includeFontPadding = false
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        return row
    }

    /** Misma regla que el ticket ESC/POS: identificador si no es para llevar o si no hay preferencia definida. */
    private fun shouldShowIdentifier(ticket: NonFiscalTicket): Boolean {
        if (ticket.identifier?.trim()?.isNotEmpty() != true) return false
        return ticket.isTakeAway != true
    }

    /** Meta superior (preferencia, identificador, descuento) — mismo orden que `_nonFiscalTicketMetaItemsAndTotals`. */
    private fun addNonFiscalMetaBlock(layout: LinearLayout, ticket: NonFiscalTicket) {
        if (ticket.isTakeAway != null) {
            val pref = if (ticket.isTakeAway == true) "Para llevar" else "Para comer aquí"
            layout.addView(createMetaBoldLine("Preferencia: $pref"))
        }
        if (shouldShowIdentifier(ticket)) {
            layout.addView(createMetaBoldLine("Identificador: ${ticket.identifier!!.trim()}"))
        }
        if (NonFiscalReceiptTotals.hasDiscountMeta(ticket)) {
            val line = NonFiscalReceiptTotals.buildDiscountMetaLine(ticket, ::formatCurrency)
            if (line.isNotEmpty()) {
                layout.addView(createMetaBoldLine(line))
            }
        }
    }

    private fun createMetaBoldLine(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            typeface = robotoMonoBold
            textSize = if (is55mmPaper) 20f else 27f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, if (is55mmPaper) 2 else 4, 0, if (is55mmPaper) 2 else 4)
            }
        }
    }

    /** `hr(ch: ' ')`, `hr()`, `hr(ch: ' ')` antes del detalle cuando hay preferencia (referencia POS). */
    private fun addTripleSpaceDashSpace(layout: LinearLayout) {
        layout.addView(createSeparatorLine(" ", lineCharCount))
        layout.addView(createSeparatorLine("-", lineCharCount))
        layout.addView(createSeparatorLine(" ", lineCharCount))
    }

    /**
     * Evita cortes ridículos en montos (`-$ ` / `$ ` → NBSP) para que el símbolo y el número
     * salten de línea juntos si hace falta.
     */
    private fun strengthenCurrencyForWrap(value: String): String =
        value.replace("-$ ", "-$\u00A0").replace("$ ", "$\u00A0")

    /**
     * Salto de línea “limpio”: sin guiones automáticos; en API 29+ equilibrado; en API 33+
     * [LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE] para no partir frases de un carácter.
     */
    private fun applyReceiptTotalRowTextWrap(tv: TextView) {
        tv.isSingleLine = false
        tv.maxLines = 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tv.breakStrategy = Layout.BREAK_STRATEGY_BALANCED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tv.lineBreakWordStyle =
                android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        }
    }

    /**
     * Fila `etiqueta` ← → `monto`. Ambos pueden hacer varias líneas; el monto va alineado a la derecha.
     * Se aplican reglas de salto para evitar guiones raros y cortes de una sola letra en frases.
     */
    private fun createLabelValueTotalRow(label: String, value: String, compact: Boolean = false): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, if (is55mmPaper) 3 else 6, 0, if (is55mmPaper) 3 else 6)
            }
        }
        val textSizeSp = when {
            compact && is55mmPaper -> 18f
            compact -> 24f
            is55mmPaper -> 22f
            else -> 30f
        }
        row.addView(TextView(context).apply {
            text = label
            typeface = robotoMonoBold
            textSize = textSizeSp
            includeFontPadding = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
            applyReceiptTotalRowTextWrap(this)
        })
        row.addView(TextView(context).apply {
            text = strengthenCurrencyForWrap(value)
            typeface = robotoMonoBold
            textSize = textSizeSp
            includeFontPadding = false
            gravity = Gravity.END or Gravity.TOP
            textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = if (is55mmPaper) 4 else 8
            }
            applyReceiptTotalRowTextWrap(this)
        })
        return row
    }

    private fun addTotalsBlock(layout: LinearLayout, ticket: NonFiscalTicket) {
        val r = NonFiscalReceiptTotals.resolve(ticket)
        if (r.showBreakdown) {
            layout.addView(createLabelValueTotalRow("Subtotal:", formatCurrency(r.subtotal), compact = true))
            layout.addView(createLabelValueTotalRow("Descuento:", "-${formatCurrency(r.discount)}", compact = true))
            layout.addView(createLabelValueTotalRow("Total con descuento:", formatCurrency(r.total), compact = true))
        } else {
            layout.addView(createLabelValueTotalRow("Total:", formatCurrency(r.total)))
        }
    }
}