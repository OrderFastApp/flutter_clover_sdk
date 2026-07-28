package ar.com.orderfast.printing.templates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.com.orderfast.models.ticket.FiscalTicket
import ar.com.orderfast.models.ticket.PaperProfile
import ar.com.orderfast.printing.styles.PrintTicketStyles
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class FiscalTicketTemplate(private val context: Context) {

    fun buildView(ticket: FiscalTicket, paperProfile: PaperProfile): View {
        val styles = PrintTicketStyles(context, paperProfile)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val info = ticket.fiscalInfo

        // Header Fiscal AFIP
        val tipoComp = ticket.tipoComprobante ?: "Factura B"
        layout.addView(styles.createTextView(tipoComp.uppercase(), bold = true, large = true, gravityVal = Gravity.CENTER))
        layout.addView(styles.createHr("=", styles.lineCharCount))

        layout.addView(styles.createTextView(info.razonSocial, bold = true, gravityVal = Gravity.START))
        layout.addView(styles.createTextView("CUIT: ${info.cuit}", gravityVal = Gravity.START))
        layout.addView(styles.createTextView("IIBB: ${info.numeroInscripcionIIBB}", gravityVal = Gravity.START))
        layout.addView(styles.createTextView(info.direccion, gravityVal = Gravity.START))
        layout.addView(styles.createTextView(info.localidad, gravityVal = Gravity.START))
        layout.addView(styles.createTextView("Condición: ${info.responsable}", gravityVal = Gravity.START))
        layout.addView(styles.createTextView("Inicio Act.: ${info.inicioActividades}", gravityVal = Gravity.START))

        layout.addView(styles.createHr("-", styles.lineCharCount))
        layout.addView(styles.createTextView("P. Venta: ${info.puntoVenta}  Comp. N°: ${info.numeroT}"))
        layout.addView(styles.createTextView("Fecha: ${info.fecha}"))

        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Tabla de ítems
        layout.addView(styles.tableLine("CANT", "DESCRIPCION", "SUBT", "TOTAL", bold = true))
        layout.addView(styles.createHr("-", styles.lineCharCount))

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
        }

        layout.addView(styles.createHr("-", styles.lineCharCount))

        // Desglose de impuestos
        if (info.ivaContenido != null) {
            layout.addView(styles.createTextView("IVA Contenido (21%): ${styles.formatCurrency(info.ivaContenido)}", gravityVal = Gravity.END))
        }

        val totalStr = styles.formatCurrency(ticket.total)
        layout.addView(styles.createTextView("TOTAL: $totalStr", bold = true, large = true, gravityVal = Gravity.END))

        layout.addView(styles.createHr("=", styles.lineCharCount))

        // Pie Fiscal (CAE)
        if (!info.cae.isNullOrEmpty()) {
            layout.addView(styles.createTextView("CAE N°: ${info.cae}", bold = true, gravityVal = Gravity.CENTER))
            layout.addView(styles.createTextView("Vencimiento CAE: ${info.fechaVencimiento}", gravityVal = Gravity.CENTER))
        }

        // QR Code AFIP
        if (!info.qrCodeData.isNullOrEmpty()) {
            val qrSize = if (styles.is55mm) 180 else 240
            val qrBitmap = generateQrBitmap(info.qrCodeData, qrSize)
            if (qrBitmap != null) {
                val qrImageView = ImageView(context).apply {
                    setImageBitmap(qrBitmap)
                    layoutParams = LinearLayout.LayoutParams(qrSize, qrSize).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 4, 0, 4)
                    }
                }
                layout.addView(qrImageView)
            }
        }

        if (!info.regimenFiscal.isNullOrEmpty()) {
            layout.addView(styles.createTextView(info.regimenFiscal, small = true, gravityVal = Gravity.CENTER))
        }

        return layout
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
