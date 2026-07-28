package ar.com.orderfast.models.ticket

enum class PaperProfile {
    MM80,
    MM55,
    AUTO;

    companion object {
        fun fromString(value: String?): PaperProfile {
            return when (value?.lowercase()) {
                "mm80", "80mm", "80" -> MM80
                "mm55", "55mm", "55" -> MM55
                else -> AUTO
            }
        }
    }
}
