package ar.com.orderfast.printing.styles

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import ar.com.orderfast.R
import ar.com.orderfast.models.ticket.PaperProfile
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class PrintTicketStyles(
    private val context: Context,
    val profile: PaperProfile
) {
    val is55mm: Boolean = profile == PaperProfile.MM55

    val printerWidthPx: Int = if (is55mm) 384 else 576
    val lineCharCount: Int = if (is55mm) 22 else 32

    val robotoMono: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.roboto_mono) ?: Typeface.MONOSPACE
    }

    val robotoMonoBold: Typeface by lazy {
        Typeface.create(robotoMono, Typeface.BOLD)
    }

    val textSizeSmall: Float = if (is55mm) 12f else 14f
    val textSizeStandard: Float = if (is55mm) 14f else 17f
    val textSizeLarge: Float = if (is55mm) 18f else 22f
    val textSizeTitle: Float = if (is55mm) 22f else 28f

    data class Columns(
        val qty: Int,
        val desc: Int,
        val subtotal: Int,
        val total: Int
    ) {
        val totalChars: Int get() = qty + desc + subtotal + total
    }

    val columns: Columns
        get() = if (is55mm) {
            Columns(qty = 3, desc = 13, subtotal = 9, total = 9)
        } else {
            Columns(qty = 5, desc = 22, subtotal = 12, total = 12)
        }

    private val argSymbols: DecimalFormatSymbols by lazy {
        DecimalFormatSymbols(Locale("es", "AR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
    }

    private val numberFormat: NumberFormat by lazy { DecimalFormat("#,##0.00", argSymbols) }
    private val integerNumberFormat: NumberFormat by lazy { DecimalFormat("#,##0", argSymbols) }

    fun formatCurrency(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        val formatter = if (cents % 100L == 0L) integerNumberFormat else numberFormat
        return "\$ ${formatter.format(amount)}"
    }

    fun fixedCell(value: String, width: Int, alignEnd: Boolean = false): String {
        val clean = value.replace("\n", " ").trim()
        val truncated = if (clean.length > width) {
            clean.take((width - 1).coerceAtLeast(1)) + "…"
        } else {
            clean
        }
        return if (alignEnd) truncated.padStart(width) else truncated.padEnd(width)
    }

    fun createHr(char: String = "-", length: Int = lineCharCount): TextView {
        return TextView(context).apply {
            text = char.repeat(length)
            typeface = robotoMono
            textSize = textSizeStandard
            gravity = Gravity.CENTER_HORIZONTAL
            includeFontPadding = false
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun createInvertedBadge(textVal: String): TextView {
        return TextView(context).apply {
            text = "  $textVal  "
            typeface = robotoMonoBold
            textSize = textSizeLarge
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 8, 0, 8)
            }
        }
    }

    fun createTextView(
        textVal: String,
        bold: Boolean = false,
        large: Boolean = false,
        gravityVal: Int = Gravity.START,
        small: Boolean = false
    ): TextView {
        val calculatedSize = when {
            large -> textSizeLarge
            small -> textSizeSmall
            else -> textSizeStandard
        }
        return TextView(context).apply {
            text = textVal
            typeface = if (bold) robotoMonoBold else robotoMono
            textSize = calculatedSize
            gravity = gravityVal
            includeFontPadding = false
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun tableLine(
        qty: String,
        desc: String,
        subtotal: String,
        total: String,
        bold: Boolean = false
    ): TextView {
        val c = columns
        val line = fixedCell(qty, c.qty) +
                fixedCell(desc, c.desc) +
                fixedCell(subtotal, c.subtotal, alignEnd = true) +
                fixedCell(total, c.total, alignEnd = true)

        return TextView(context).apply {
            text = line
            typeface = if (bold) robotoMonoBold else robotoMono
            textSize = textSizeStandard
            includeFontPadding = false
            setPadding(2, 2, 2, 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
