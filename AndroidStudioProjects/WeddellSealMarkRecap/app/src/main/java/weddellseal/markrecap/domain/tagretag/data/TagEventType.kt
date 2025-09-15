package weddellseal.markrecap.domain.tagretag.data

enum class TagEventType(val alpha: String, val description: String) {
    MARKED("M", "Marked"),
    NEW("N", "New"),
    RETAG("R2", "Retag"),
    UNKNOWN("", "Unknown"); // initial value

    companion object {
        // Convert alpha string to enum value
        fun fromAlpha(alpha: String?): TagEventType =
            values().find { it.alpha == alpha } ?: UNKNOWN

        // Convert label string to enum value
        fun fromSelection(description: String): TagEventType =
            values().find { it.description == description } ?: UNKNOWN
    }
}