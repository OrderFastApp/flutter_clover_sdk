package ar.com.orderfast.services

import android.accounts.Account
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import ar.com.orderfast.models.ticket.FiscalTicket
import ar.com.orderfast.models.ticket.KitchenTicketDto
import ar.com.orderfast.models.ticket.NonFiscalTicket
import ar.com.orderfast.models.ticket.PaperProfile
import ar.com.orderfast.models.ticket.PrintTicketResponse
import ar.com.orderfast.printing.styles.PrintTicketStyles
import ar.com.orderfast.printing.templates.CashTicketTemplate
import ar.com.orderfast.printing.templates.CustomerTicketTemplate
import ar.com.orderfast.printing.templates.FiscalTicketTemplate
import ar.com.orderfast.printing.templates.KitchenTicketTemplate
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v1.printer.job.ImagePrintJob2
import com.clover.sdk.v1.printer.job.ViewPrintJob
import com.clover.sdk.v3.merchant.MerchantDevicesV2Connector
import java.util.Locale

/**
 * Servicio para manejar la impresión de tickets con Clover.
 * Soporta detección automática de hardware (80mm vs 55mm), modelos consolidados y plantillas modulares.
 */
class PrintService(private val context: Context) {

    companion object {
        private const val TAG = "PrintService"
        private const val IMAGE_MAX_HEIGHT = 2048
    }

    // HandlerThread para operaciones de impresión en segundo plano
    private val printThread = HandlerThread("PrintThread").apply { start() }
    private val printHandler = Handler(printThread.looper)

    /**
     * Detección física automática del modelo de hardware Clover.
     */
    fun detectPhysicalPaperProfile(): PaperProfile {
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
            Log.w(TAG, "No se pudo detectar el modelo Clover automáticamente, se usa MM80 por defecto", e)
            PaperProfile.MM80
        }
    }

    /**
     * Resuelve el perfil final de papel (si es AUTO, detecta el dispositivo físico).
     */
    fun resolvePaperProfile(requested: PaperProfile): PaperProfile {
        return if (requested == PaperProfile.AUTO) {
            detectPhysicalPaperProfile()
        } else {
            requested
        }
    }

    /**
     * Imprime una Vista Android midiendo y usando ViewPrintJob.
     */
    private fun printView(
        account: Account,
        paperProfile: PaperProfile,
        buildView: () -> View,
        successMessage: String,
        logMessage: String,
        callback: (PrintTicketResponse) -> Unit,
    ) {
        printHandler.post {
            var rootView: View? = null
            try {
                val resolvedProfile = resolvePaperProfile(paperProfile)
                val styles = PrintTicketStyles(context, resolvedProfile)
                val printerWidth = styles.printerWidthPx

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

                Log.d(TAG, "$logMessage (Perfil: ${resolvedProfile.name}, Ancho: ${printerWidth}px)")
                callback(PrintTicketResponse(success = true, message = successMessage))
            } catch (e: Exception) {
                Log.e(TAG, "Error al imprimir ticket", e)
                callback(
                    PrintTicketResponse(
                        success = false,
                        error = e.message ?: "Error desconocido al imprimir",
                    )
                )
            } finally {
                rootView?.let { recycleImageViewBitmaps(it) }
            }
        }
    }

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

    // =========================================================================
    // MÉTODOS DE IMPRESIÓN BASADOS EN PLANTILLAS
    // =========================================================================

    /**
     * Imprime un ticket no fiscal (Comprobante de Venta / Cliente) usando [CustomerTicketTemplate]
     */
    fun printNonFiscalTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(success = false, error = "No se pudo obtener la cuenta de Clover"))
                return
            }
        val resolvedProfile = resolvePaperProfile(ticket.paperProfile)
        val template = CustomerTicketTemplate(context)

        printView(
            account = account,
            paperProfile = resolvedProfile,
            buildView = { template.buildView(ticket, resolvedProfile) },
            successMessage = "Ticket no fiscal impreso correctamente",
            logMessage = "Ticket no fiscal enviado a impresión: ${ticket.orderNumber}",
            callback = callback
        )
    }

    /**
     * Imprime un ticket de caja usando [CashTicketTemplate]
     */
    fun printCashTicket(ticket: NonFiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(success = false, error = "No se pudo obtener la cuenta de Clover"))
                return
            }
        val resolvedProfile = resolvePaperProfile(ticket.paperProfile)
        val template = CashTicketTemplate(context)

        printView(
            account = account,
            paperProfile = resolvedProfile,
            buildView = { template.buildView(ticket, resolvedProfile) },
            successMessage = "Cash ticket impreso correctamente",
            logMessage = "Cash ticket enviado a impresión: ${ticket.orderNumber}",
            callback = callback
        )
    }

    /**
     * Imprime una comanda de cocina usando [KitchenTicketTemplate]
     */
    fun printKitchenTicket(dto: KitchenTicketDto, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(success = false, error = "No se pudo obtener la cuenta de Clover"))
                return
            }
        val resolvedProfile = resolvePaperProfile(dto.paperProfile)
        val template = KitchenTicketTemplate(context)

        printView(
            account = account,
            paperProfile = resolvedProfile,
            buildView = { template.buildView(dto, resolvedProfile) },
            successMessage = "Kitchen Ticket impreso correctamente",
            logMessage = "Kitchen Ticket enviado a impresión: ${dto.orderNumber}",
            callback = callback
        )
    }

    /**
     * Imprime un ticket fiscal AFIP usando [FiscalTicketTemplate]
     */
    fun printFiscalTicket(ticket: FiscalTicket, callback: (PrintTicketResponse) -> Unit) {
        val account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(success = false, error = "No se pudo obtener la cuenta de Clover"))
                return
            }
        val resolvedProfile = resolvePaperProfile(ticket.paperProfile)
        val template = FiscalTicketTemplate(context)

        printView(
            account = account,
            paperProfile = resolvedProfile,
            buildView = { template.buildView(ticket, resolvedProfile) },
            successMessage = "Ticket fiscal impreso correctamente",
            logMessage = "Ticket fiscal enviado a impresión",
            callback = callback
        )
    }

    // =========================================================================
    // IMPRESIÓN DE IMÁGENES BASE64
    // =========================================================================

    fun printImage(base64: String, callback: (PrintTicketResponse) -> Unit) {
        val account: Account = CloverAccount.getAccount(context)
            ?: run {
                callback(PrintTicketResponse(success = false, error = "No se pudo obtener la cuenta de Clover"))
                return
            }

        printHandler.post {
            try {
                val cleanBase64 = base64.substringAfter(",").trim().ifEmpty { base64 }
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: run {
                        callback(PrintTicketResponse(success = false, error = "No se pudo decodificar la imagen base64"))
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
                callback(PrintTicketResponse(success = true, message = "Imagen impresa correctamente"))
            } catch (e: Exception) {
                Log.e(TAG, "Error al imprimir imagen", e)
                callback(PrintTicketResponse(success = false, error = e.message ?: "Error desconocido al imprimir imagen"))
            }
        }
    }
}