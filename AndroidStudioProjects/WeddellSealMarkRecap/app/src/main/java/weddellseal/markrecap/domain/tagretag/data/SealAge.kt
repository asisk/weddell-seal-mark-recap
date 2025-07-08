package weddellseal.markrecap.domain.tagretag.data

enum class SealAge(val alpha: String, val description: String) {
    ADULT("A", "Adult"),
    PUP("P", "Pup"),
    YEARLING("Y", "Yearling"),
    UNKNOWN("", "Unknown"); // initialization value only

    companion object {
        // Convert alpha string to enum value
        // WedCheck records should have an age of UNKNOWN
        // ex. "A" → SealAge.ADULT
        fun fromAlpha(alpha: String?): SealAge =
            SealAge.values().find { it.alpha == alpha } ?: UNKNOWN

        // Convert label string to enum value
        // Used to set the Age of a Seal when selecting from the SegmentedButtonGroup
        // ex. "Adult" → SealAge.ADULT
        fun fromSelection(description: String): SealAge =
            SealAge.values().find { it.description == description } ?: UNKNOWN
    }

}

// Extension function to convert SealAge to display list
// Used for the SegmentedButtonGroup in the TagRetag screen
fun SealAge.toList(): List<String> {
    return SealAge.values()
        .filter { it != SealAge.UNKNOWN}
        .map { it.description }
}
