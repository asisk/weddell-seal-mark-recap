package weddellseal.markrecap.domain.tagretag.data

enum class SealAgeClass(val alpha: String, val description: String) {
    ADULT("A", "Adult"),
    PUP("P", "Pup"),
    YEARLING("Y", "Yearling"),
    UNKNOWN("", "Unknown"); // initial value

    companion object {
        // Convert alpha string to enum value
        // WedCheck records should have an age class defined by an alpha character
        // ex. "A" → SealAge.ADULT
        fun fromAlpha(alpha: String?): SealAgeClass =
            SealAgeClass.values().find { it.alpha == alpha } ?: UNKNOWN

        // Convert label string to enum value
        // Used to set the Age of a Seal when selecting from the SegmentedButtonGroup
        // ex. "Adult" → SealAge.ADULT
        fun fromSelection(description: String): SealAgeClass =
            SealAgeClass.values().find { it.description == description } ?: UNKNOWN
    }
}