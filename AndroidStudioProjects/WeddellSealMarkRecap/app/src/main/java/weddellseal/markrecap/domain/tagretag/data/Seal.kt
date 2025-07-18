package weddellseal.markrecap.domain.tagretag.data

import weddellseal.markrecap.ui.utils.getCurrentYear

data class Seal(
    val sealType: SealType = SealType.UNKNOWN, // TODO, implement this in place of name
    val age: String = "", //TODO, replace with enum
    val ageYears: String = "",
    val colony: String = "",
    val comment: String = "",
    val condition: SealCondition = SealCondition.UNKNOWN,
    var isNoTag: Boolean = false,
    val lastPhysio: String = "",
    val name: String = "", //TODO, replace with enum
    val notebookDataString: String = "",
    val numRelatives: String = "", //TODO, replace with enum
    val numTags: String = "",
    val numTagsMatch: Boolean = false,
    val photoYears: String = "",
    val previousPups: String = "",
    val pupPeed: Boolean = false,
    val reasonForRetag: RetagReason = RetagReason.UNKNOWN,
    val sex: String = "", //TODO, replace with enum
    val sexMatch: Boolean = false,
    val swimPups: String = "",
    val tagEventType: String = "", //TODO, replace with enum
    val tagNumber: String = "",
    val tagAlpha: String = "",
    val oldTagNumber: String = "",
    val oldTagAlpha: String = "",
    val oldTagMarks: Boolean = false,
    val tissueTaken: Boolean = false,
    val tissue: String = "",
    val weight: Int = 0,
    val weightTaken: Boolean = false,
    val observationID: Int = 0, // represents the record ID for an existing observation when mapping an ObservationRecord to a Seal
    val isTagRetagEntry: Boolean = false,
    val observationRecordSpeno: Int = 0,
    val wedCheckMatch: WedCheckSeal? = null, // This could be null if there is no match in the database
    var flaggedForReview: Boolean = false,
) {
    // This is a check to see whether the user has begun data entry
    // Only checking primary fields, not fields that appear as a result of a field being selected
    val isEntryStarted: Boolean
        get() = listOf(
            age.isNotBlank(),
            sex.isNotBlank(),
            numRelatives.isNotBlank(),
            condition != SealCondition.UNKNOWN && condition != SealCondition.NONE,
            tagEventType.isNotBlank(),
            tagNumber.isNotBlank(),
            tagAlpha.isNotBlank(),
            numTags.isNotBlank(),
            isNoTag, // No tag is selected
            tissueTaken,
            comment.isNotBlank(),
        ).any { it }


    val isComplete: Boolean
        get() = completenessReasons.isEmpty()

    // Required fields to enable Save button
    val completenessReasons: List<String>
        get() {
            val reasons = mutableListOf<String>()

            if (!isEntryStarted) return reasons // early return, skip validation checks when entry hasn't begun for this seal

            // --- Basic Required Fields ---
            if (age.isEmpty()) reasons += "Select an age for $name."
            // pup condition will be UNKNOWN when first instantiated
            // a condition of NONE means the value was selected as a way to set the value to blank from the TagRetag Screen
            if (age == "Pup" && (condition == SealCondition.NONE || condition == SealCondition.UNKNOWN)) reasons += "Select condition for Pup ($name)."
            if (sex.isEmpty()) reasons += "Select a sex for $name."
            if (numRelatives.isEmpty()) reasons += "Select number of relatives for $name."
            if (tagEventType.isEmpty()) reasons += "Select a tag event type for $name."
            if (tagEventType == "Retag" && (reasonForRetag == RetagReason.NONE || reasonForRetag == RetagReason.UNKNOWN)) reasons += "Enter a reason for retag for $name."

            // --- Tag Number ---
            if (!isNoTag) {
                if (tagNumber.isEmpty()) {
                    reasons += "Enter a tag number for $name."
                } else if (tagNumber.length !in 3..4) { //If tagNumber is not 3 or 4 characters long
                    reasons += "Tag number must be 3 or 4 digits for $name."
                }

                // We don't need any validation on the number of tags during a retag event (since that's typically why we are retagging them).
                // We'll still want validation on sex, age class, colony, etc. - just not the number of tags.
                // Number of tags is required when a tag number is entered
                if (tagEventType != "Retag" && numTags.isEmpty()) {
                    reasons += "Select number of tags for $name."
                }
            }

            return reasons
        }

    val hasPupOne: Boolean
        get() = numRelatives == "1"

    val hasPupTwo: Boolean
        get() = numRelatives == "2"

    val isTagIDValid: Boolean
        get() = tagNumber.isNotEmpty() && tagAlpha.isNotEmpty() && tagNumber.length in 3..4

    val isOldTagValid: Boolean
        get() = oldTagNumber.isNotEmpty() && oldTagAlpha.isNotEmpty() && oldTagNumber.length in 2..4

    val useTagID: Boolean
        get() = tagEventType == "New" || tagEventType == "Marked" || tagEventType.isEmpty()

    val useOldTag: Boolean
        get() = tagEventType == "Retag"

    val hasWedCheckMatch: Boolean
        get() = wedCheckMatch != null

    val isValid: Boolean
        get() = isComplete && validationErrors.isEmpty()

    val validationMessage: String
        get() = validationErrors.joinToString("\n")

    val validationErrors: List<String>
        get() {
            val errors = mutableListOf<String>()
            val currentYear = getCurrentYear()

            if (isNoTag) return errors // early return, skip all validation checks when no tag is entered

            if (!isComplete) return errors // early return, skip validation checks when required fields are not completed

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
            //TODO, more testing with retag
            wedCheckMatch.let { record ->

                if (sex != "Unknown" && sex != record.sex) {
                    // ----- Validation Rule -----
                    // Sex entered must match WedCheck entry unless the entered seal sex is "Unknown"
                    errors += "Sex doesn't match. WedCheck record has sex recorded as ${record.sex}."
                }

                // We don't need any validation on the number of tags during a retag event (since that's typically why we are retagging them).
                // We'll still want validation on sex, age class, colony, etc. - just not the number of tags.
                if (tagEventType != "Retag" && numTags != record.numTags) {
                    // ----- Validation Rule -----
                    errors += "Number of tags doesn't match. WedCheck record has ${record.numTags} tags."
                }

                // If the seal was last seen dead, the condition code should be 0
                if (record.condition == SealCondition.DEAD && condition != SealCondition.DEAD) {
                    // ----- Validation Rule -----
                    // dead seals cannot be revived to the living! ;)
                    errors += "Seal last seen dead!"
                }

                if (record.lastSeenSeason < (currentYear - 10)) {
                    // ----- Validation Rule -----
                    // new observations for seals seen more than ten years ago are unlikely
                    errors += "Seal last seen more than ten years ago. WedCheck last seen season is ${record.lastSeenSeason}."
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
