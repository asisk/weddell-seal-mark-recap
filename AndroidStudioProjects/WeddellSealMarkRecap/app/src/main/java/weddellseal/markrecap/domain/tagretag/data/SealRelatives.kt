package weddellseal.markrecap.domain.tagretag.data

enum class SealRelatives(val value: Int, val label: String) {
    ZERO(0, "0"),
    ONE(1, "1"),
    TWO(2, "2"),

    UNKNOWN(-1, "");

    companion object {
        // Convert integer value to enum value
        // ex. 2 → SealRelatives.TWO
        fun fromIntVal(value: Int?): SealRelatives =
            values().find { it.value == value } ?: UNKNOWN

        // Convert label string to enum value
        // ex. "2" → SealRelatives.TWO
        fun fromSelection(label: String): SealRelatives =
            values().find { it.label == label } ?: UNKNOWN
    }
}
