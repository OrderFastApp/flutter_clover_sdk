package ar.com.orderfast.models.ticket

/**
 * Subselección de un item (modificador, opción adicional, etc.)
 */
data class TicketSubselection(
    val name: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)

/**
 * Item de un ticket
 */
data class TicketItem(
    val quantity: Int,
    val description: String,
    val subtotal: Double,
    val total: Double,
    val comment: String? = null,
    val subselections: List<TicketSubselection>? = null
)

/**
 * Información fiscal del ticket AFIP
 */
data class FiscalInfo(
    val razonSocial: String,
    val cuit: String,
    val direccion: String,
    val localidad: String,
    val numeroInscripcionIIBB: String,
    val responsable: String,
    val inicioActividades: String,
    val fecha: String,
    val numeroT: String,
    val puntoVenta: String,
    val consumidorFinal: Boolean = true,
    val regimenFiscal: String? = null,
    val ivaContenido: Double? = null,
    val otrosImpuestosNacionales: Double? = null,
    val cae: String? = null,
    val fechaVencimiento: String? = null,
    val qrCodeData: String? = null
)

/**
 * Ticket no fiscal (Comprobante de Venta / Cliente / Caja)
 */
data class NonFiscalTicket(
    val orderNumber: String,
    val items: List<TicketItem>,
    val total: Double,
    val dateTime: String,
    val storeName: String? = null,
    val isTakeAway: Boolean? = null,
    val identifier: String? = null,
    val disclaimer: String? = "Comprobante no válido como factura",
    val discountCode: String? = null,
    val discountPercent: Double? = null,
    val discountAmount: Double? = null,
    val subtotalBeforeDiscount: Double? = null,
    val discountAppliedAmount: Double? = null,
    val paperProfile: PaperProfile = PaperProfile.AUTO
)

/**
 * Ticket fiscal (AFIP)
 */
data class FiscalTicket(
    val orderNumber: String? = null,
    val items: List<TicketItem>,
    val total: Double,
    val dateTime: String,
    val fiscalInfo: FiscalInfo,
    val tipoComprobante: String? = null,
    val paperProfile: PaperProfile = PaperProfile.AUTO
)

/**
 * Response de impresión
 */
data class PrintTicketResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)
