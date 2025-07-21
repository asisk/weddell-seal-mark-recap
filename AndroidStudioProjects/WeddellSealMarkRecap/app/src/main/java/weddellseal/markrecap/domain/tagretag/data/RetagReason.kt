package weddellseal.markrecap.domain.tagretag.data

enum class RetagReason (val code: String, val description: String) {
    NONE("0", "None"),
    ONE_OF_FOUR("1", "1 of 4"),
    TWO_OF_FOUR("2", "2 of 4"),
    THREE_OF_FOUR("3", "3 of 4"),
    WORN("4", "Worn"),
    BROKEN("5", "Broken"),
    OTHER("6", "Other"),
    UNKNOWN("", "Unknown"); // the retag option was never set, so the database value should be blank

    companion object {
        // Convert code string to enum value
        // ex. "3" → RetagReason.THREE_OF_FOUR
        fun fromCode(code: String?): RetagReason =
            RetagReason.values().find { it.code == code } ?: NONE

        // Convert label string to enum value
        // ex. "Worn" → RetagReason.WORN
        fun fromLabel(description: String): RetagReason =
            RetagReason.values().find { it.description == description } ?: NONE
    }
}

// Extension function to convert RetagReason to display string
// This is used for the dropdown UI when showing the current selection
fun RetagReason.toLabel(): String {
    if (this == RetagReason.UNKNOWN) {
        return "Select Reason"
    }
    return this.description
}