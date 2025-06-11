package weddellseal.markrecap.domain.tagretag.data

data class Seal(
    val age: String = "",
    val ageYears: String = "",
    val colony: String = "",
    val comment: String = "",
    val condition: String = "",
    var flaggedForReview: Boolean = false,
    val hasWedCheckSpeno: Boolean = false,
    var isStarted: Boolean = false,
    var isNoTag: Boolean = false,
    val isValidated: Boolean = false,
    var isWedCheck: Boolean = false,
    val lastObservedDead: Boolean = false,
    val lastPhysio: String = "",
    val lastSeenSeason: Int = 0,
    val lastSeasonWithinTenYears: Boolean = false,
    val massPups: String = "",
    val name: String = "",
    val notebookDataString: String = "",
    val numRelatives: String = "",
    val numTags: String = "",
    val numTagsMatch: Boolean = false,
    val oldTagId: String = "",
    val oldTagMarks: Boolean = false,
    val photoYears: String = "",
    val previousPups: String = "",
    val pupPeed: Boolean = false,
    val reasonForRetag: String = "",
    val reasonNotValid: String = "",
    val sex: String = "",
    val sexMatch: Boolean = false,
    val speNo: Int = 0,
    val swimPups: String = "",
    val tagEventType: String = "",
    val tagAlpha: String = "",
    val tagNumber: String = "",
    val tissueTaken: Boolean = false,
    val tissue: String = "",
    val weight: Int = 0,
    val weightTaken: Boolean = false,
    val observationID : Int = 0,
    val isObservationLogEntry : Boolean = false,
){
    val isComplete: Boolean
        get() = completenessReasons.isEmpty()

    val completenessReasons: List<String>
        get() {
            val reasons = mutableListOf<String>()

            // Required fields to enable Save button
            if (age.isEmpty()) reasons += "Select an age for $name."
            if (age == "Pup" && condition.isEmpty()) reasons += "Select condition for Pup ($name)."
            if (sex.isEmpty()) reasons += "Select a sex for $name."
            if (numRelatives.isEmpty()) reasons += "Select number of relatives for $name."
            if (tagEventType.isEmpty()) reasons += "Select a tag event type for $name."
            if (!isNoTag) {
                if (tagNumber.isEmpty()) reasons += "Enter a tag number for $name."
                if (numTags.isEmpty()) reasons += "Select number of tags for $name."
            }

            return reasons
        }

    val isValid: Boolean
        get() = validationErrors.isEmpty()

    val validationErrors: List<String>
        get() {
            val errors = mutableListOf<String>()

            if (!isNoTag && tagNumber.length !in 3..4) {
                errors += "Tag number must be 3 or 4 digits for $name."
            }

            if (age.isEmpty()) {
                errors += "Select an age for $name."
            } else if (age == "Pup" && condition.isEmpty()) {
                errors += "Select condition for Pup ($name)."
            }

            if (sex.isEmpty()) {
                errors += "Select a sex for $name."
            }

            if (numRelatives.isEmpty()) {
                errors += "Select number of relatives for $name."
            }

            if (tagEventType.isEmpty()) {
                errors += "Select a tag event for $name."
            }

            if (!isNoTag) {
                if (tagNumber.length !in 3..4) {
                    errors += "Tag number for $name must be 3 or 4 digits."
                }
                if (numTags.isEmpty()) {
                    errors += "Select number of tags for $name."
                }
            }

            return errors
        }

    val validationMessage: String
        get() = validationErrors.joinToString("\n")
}