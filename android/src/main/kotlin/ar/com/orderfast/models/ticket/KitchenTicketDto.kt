package ar.com.orderfast.models.ticket

data class KitchenTicketDto(
    val orderNumber: String,
    val items: List<TicketItem>,
    val dateTime: String,
    val isTakeAway: Boolean? = null,
    val identifier: String? = null,
    val notes: String? = null,
    val paperProfile: PaperProfile = PaperProfile.AUTO
)
