package weddellseal.markrecap.domain.tagretag.data

enum class SealSex(val alpha: String, val description: String) {
    FEMALE("F", "Female"),
    MALE("M", "Male"),
    UNKNOWN("U", "Unknown"),
    NONE("", "None");

    companion object {
        // Convert alpha string to enum value
        fun fromAlpha(alpha: String?): SealSex =
            values().find { it.alpha == alpha } ?: UNKNOWN

        // Convert label string to enum value
        fun fromSelection(description: String): SealSex =
            values().find { it.description == description } ?: UNKNOWN
    }
}