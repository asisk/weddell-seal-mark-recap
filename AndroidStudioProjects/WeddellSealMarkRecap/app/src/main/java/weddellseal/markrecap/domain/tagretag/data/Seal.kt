package weddellseal.markrecap.domain.tagretag.data

import weddellseal.markrecap.ui.utils.getCurrentYear

data class Seal(
    val age: String = "",
    val ageYears: String = "",
    val colony: String = "",
    val comment: String = "",
    val condition: SealCondition = SealCondition.UNKNOWN,
    var isNoTag: Boolean = false, // when marked, this clears the tag number, if entered
    val lastPhysio: String = "",
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
    val sex: String = "",
    val sexMatch: Boolean = false,
    val swimPups: String = "",
    val tagEventType: String = "",
    val tagAlpha: String = "",
    val tagNumber: String = "",
    val tissueTaken: Boolean = false,
    val tissue: String = "",
    val weight: Int = 0,
    val weightTaken: Boolean = false,
    val observationID: Int = 0, // represents the record ID for an existing observation when mapping an ObservationLogEntry to a Seal
    var isStarted: Boolean = false,
    val isObservationLogEntry: Boolean = false,
    val observationRecordSpeno: Int = 0,
    val wedCheckMatch: WedCheckSeal? = null, // This could be null if there is no match in the database
    var flaggedForReview: Boolean = false,
) {
    val isComplete: Boolean
        get() = completenessReasons.isEmpty()

    // Required fields to enable Save button
    val completenessReasons: List<String>
        get() {
            val reasons = mutableListOf<String>()

            // --- Basic Required Fields ---
            if (age.isEmpty()) reasons += "Select an age for $name."
            if (age == "Pup" && condition == SealCondition.NONE) reasons += "Select condition for Pup ($name)."
            if (sex.isEmpty()) reasons += "Select a sex for $name."
            if (numRelatives.isEmpty()) reasons += "Select number of relatives for $name."
            if (tagEventType.isEmpty()) reasons += "Select a tag event type for $name."

            // --- Tag Number ---
            if (!isNoTag) {
                if (tagNumber.isEmpty()) {
                    reasons += "Enter a tag number for $name."
                } else if (tagNumber.length !in 3..4) {
                    reasons += "Tag number must be 3 or 4 digits for $name."
                }

                // Number of tags is required when a tag number is entered
                if (numTags.isEmpty()) {
                    reasons += "Select number of tags for $name."
                }
            }

            return reasons
        }

    val isValid: Boolean
        get() = if (!isComplete) false else validationErrors.isEmpty()

    val validationMessage: String
        get() = validationErrors.joinToString("\n")

    val validationErrors: List<String>
        get() {
            val errors = mutableListOf<String>()
            val currentYear = getCurrentYear()

            if (isNoTag) return errors // early return, skip validation checks when no tag is entered

            // -----  The Seal has a tag number -----

            if (tagEventType == "New") {
                // ----- Validation Rule -----
                // new event types CANNOT have a WedCheck record
                if (wedCheckMatch != null) {
                    errors += "Tag already used! Recheck all fields before saving!\nIf you choose to save this entry, please take a photo and add a comment."
                }
                return errors // early return, only one validation rule for New event types
            }

            // ----- The tag event is Marked or Retag -----

            if (wedCheckMatch == null) {
                // ----- Validation Rule -----
                // marked and retagged seals MUST have a WedCheck record
                errors += "Seal not in database!\nPlease take a photo and add a comment."
                return errors // early return, as there's no wedcheck record to validate against
            }

            // ----- A WedCheck record is present, so validate Seal against it -----

            wedCheckMatch.let { record ->

                if (sex != "Unknown" && sex != record.sex) {
                    // ----- Validation Rule -----
                    // Sex entered must match WedCheck entry unless the entered seal sex is "Unknown"
                    errors += "Sex doesn't match"
                }

                if (numTags != record.numTags) {
                    // ----- Validation Rule -----
                    errors += "Number of tags doesn't match"
                }

                // If the seal was last seen dead, the condition code should be 0
                if (record.condition == SealCondition.DEAD && condition != SealCondition.DEAD) {
                    // ----- Validation Rule -----
                    // dead seals cannot be revived to the living! ;)
                    errors += "Seal last seen dead"
                }

                if (record.lastSeenSeason < (currentYear - 10)) {
                    // ----- Validation Rule -----
                    // new observations for seals seen more than ten years ago are unlikely
                    errors += "Seal last seen more than ten years ago"
                }

                when (record.lastSeenSeason) {
                    currentYear -> {
                        if (age != record.age) {
                            // ----- Validation Rule -----
                            // Seals entered in the current year cannot have a different age
                            errors += "Seal last observed this year. Age class can't change!"
                        }
                    }

                    currentYear - 1 -> {
                        val expectedAge = when (record.age) {
                            "Pup" -> "Yearling"
                            "Yearling" -> "Adult"
                            else -> "Adult"
                        }
                        if (age != expectedAge) {
                            // ----- Validation Rule -----
                            // Seals entered in the previous year must be advanced to the next age class
                            errors += "Seal observed last year as a ${record.age}. Age class should be $expectedAge!"
                        }
                    }

                    else -> {
                        if (age != "Adult") {
                            // ----- Validation Rule -----
                            // Seals entered two or more years ago must be Adults
                            errors += "Seal observed two or more years ago. Age class should be Adult!"
                        }
                    }
                }
            }
            return errors
        }
}

fun Seal.startingEdit(update: Seal.() -> Seal): Seal {
    return this.update().copy(isStarted = true)
}
