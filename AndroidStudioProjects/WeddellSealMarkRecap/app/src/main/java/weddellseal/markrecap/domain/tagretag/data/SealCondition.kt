package weddellseal.markrecap.domain.tagretag.data

enum class SealCondition(val code: String, val description: String) {
    DEAD("0", "Dead"),
    POOR("1", "Poor"),
    FAIR("2", "Fair"),
    GOOD("3", "Good"),
    NEWBORN("4", "Newborn"),
    NONE("", "None"),
    NA("NA", "NA"),
    UNKNOWN("", "Unknown");

    companion object {
        // Convert code string to enum value
        // ex. "3" → SealCondition.GOOD
        fun fromCode(code: String?): SealCondition =
            values().find { it.code == code } ?: NONE

        // Convert label string to enum value
        // ex. "Good - 3" → SealCondition.GOOD
        fun fromLabel(label: String): SealCondition =
            values().find { it.toLabel() == label } ?: NONE
    }
}

// Extension function to convert SealCondition to display string
// This is used for the dropdown UI when showing the current selection
// ex. SealCondition.GOOD → "Good - 3"
fun SealCondition.toLabel(): String {
    if (this == SealCondition.NONE) {
        return description
    }
    if (this == SealCondition.UNKNOWN) {
        return "Select Condition"
    }
    return "$description - $code"
}
