package weddellseal.markrecap.domain.tagretag.data

import weddellseal.markrecap.ui.utils.getCurrentYear

data class Seal(
    val sealType: SealType = SealType.UNKNOWN,
    val ageClass: SealAgeClass = SealAgeClass.UNKNOWN,
    val ageYears: String = "",
    val colony: String = "",
    val comment: String = "",
    val condition: SealCondition = SealCondition.UNKNOWN,
    var isNoTag: Boolean = false,
    val lastPhysio: String = "",
    val notebookDataString: String = "",
    val numRelatives: SealRelatives = SealRelatives.UNKNOWN,
    val numTags: String = "",
    val numTagsMatch: Boolean = false,
    val photoYears: String = "",
    val previousPups: String = "",
    val pupPeed: Boolean = false,
    val reasonForRetag: RetagReason = RetagReason.UNKNOWN,
    val sex: SealSex = SealSex.NONE,
    val sexMatch: Boolean = false,
    val swimPups: String = "",
    val tagEventType: TagEventType = TagEventType.UNKNOWN,
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
    val observationRecordSpeno: Int = 0,
    val wedCheckMatch: WedCheckSeal? = null, // This could be null if there is no match in the database
    var flaggedForReview: Boolean = false,
    var markedRemoved: Boolean = false, // when in edit mode, pups can be removed from the observations database
    var pupOneRemoved: Boolean = false, // when in edit mode, pups can be removed from the observations database
    var pupTwoRemoved: Boolean = false, // when in edit mode, pups can be removed from the observations database
    var pupAdded: Boolean = false, // when in edit mode, pups can be added to an existing record
    var hasEdits: Boolean = false,
) {
    // This is a check to see whether the user has begun data entry
    // Only checking primary fields, not fields that appear as a result of a field being selected
    val isEntryStarted: Boolean
        get() = listOf(
            ageClass != SealAgeClass.UNKNOWN,
            sex != SealSex.NONE,
            numRelatives != SealRelatives.UNKNOWN,
            condition != SealCondition.UNKNOWN && condition != SealCondition.NONE,
            tagEventType != TagEventType.UNKNOWN,
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
            if (ageClass == SealAgeClass.UNKNOWN) reasons += "Select an age for ${sealType.label}."
            // pup condition will be UNKNOWN when first instantiated
            // a condition of NONE means the value was selected as a way to set the value to blank from the TagRetag Screen
            if (ageClass == SealAgeClass.PUP && (condition == SealCondition.NONE || condition == SealCondition.UNKNOWN)) reasons += "Select condition for ${sealType.label}."
            if (sex == SealSex.NONE) reasons += "Select a sex for ${sealType.label}."
            if (numRelatives == SealRelatives.UNKNOWN && ageClass == SealAgeClass.ADULT) reasons += "Select number of relatives for ${sealType.label}."
            if (tagEventType == TagEventType.UNKNOWN) reasons += "Select a tag event type for ${sealType.label}."
            if (tagEventType == TagEventType.RETAG && (reasonForRetag == RetagReason.NONE || reasonForRetag == RetagReason.UNKNOWN)) reasons += "Enter a reason for retag for ${sealType.label}."

            // --- Tag ID ---
            // Don't validate Tag ID when No Tag is selected
            if (!isNoTag) {

                if (tagNumber.isEmpty()) {
                    reasons += "Enter a tag number for ${sealType.label}."
                } else if (tagNumber.length !in 3..4) { //If tagNumber is not 3 or 4 characters long
                    reasons += "Tag number must be 3 or 4 digits for ${sealType.label}."
                }

                if (tagAlpha.isEmpty()) {
                    reasons += "Enter a tag alpha for ${sealType.label}."
                }

                // We don't need any validation on the number of tags during a retag event (since that's typically why we are retagging them).
                // We'll still want validation on sex, age class, colony, etc. - just not the number of tags.
                // Number of tags is required when a tag number is entered
                if (tagEventType != TagEventType.RETAG && numTags.isEmpty()) {
                    reasons += "Select number of tags for ${sealType.label}."
                }
            }

            return reasons
        }

    val hasPup: Boolean
        get() {
            if (sealType != SealType.PRIMARY) return false // only the primary seal can have pups

            if (numRelatives == SealRelatives.ZERO) return false // no pups if no relatives

            if (numRelatives == SealRelatives.ONE && pupOneRemoved) return false // pupOne removed

            if (numRelatives == SealRelatives.TWO && pupOneRemoved && pupTwoRemoved) return false // all pups were removed

            return true
        }

    val hasPupOne: Boolean
        get() = numRelatives.value >= 1

    val hasPupTwo: Boolean
        get() = numRelatives == SealRelatives.TWO

    val isTagIDValid: Boolean
        get() = tagNumber.isNotEmpty() && tagAlpha.isNotEmpty() && tagNumber.length in 3..4

    val isOldTagValid: Boolean
        get() = oldTagNumber.isNotEmpty() && oldTagAlpha.isNotEmpty() && oldTagNumber.length in 2..4

    val useTagID: Boolean
        get() = tagEventType == TagEventType.NEW || tagEventType == TagEventType.MARKED || tagEventType == TagEventType.UNKNOWN

    val useOldTag: Boolean
        get() = tagEventType == TagEventType.RETAG

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

            if (tagEventType == TagEventType.NEW) {
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

                if (sex != SealSex.UNKNOWN && sex != record.sex) {
                    // ----- Validation Rule -----
                    // Sex entered must match WedCheck entry unless the entered seal sex is "Unknown"
                    errors += "Sex doesn't match. WedCheck record has sex recorded as ${record.sex}."
                }

                // We don't need any validation on the number of tags during a retag event (since that's typically why we are retagging them).
                // We'll still want validation on sex, age class, colony, etc. - just not the number of tags.
                if (tagEventType != TagEventType.RETAG && numTags != record.numTags) {
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
                        if (ageClass != record.ageClass) {
                            // ----- Validation Rule -----
                            // Seals entered in the current year cannot have a different age
                            errors += "Seal last observed this year. Age class can't change!"
                        }
                    }

                    currentYear - 1 -> {
                        val expectedAge = when (record.ageClass) {
                            SealAgeClass.PUP -> SealAgeClass.YEARLING
                            SealAgeClass.YEARLING -> SealAgeClass.ADULT
                            else -> SealAgeClass.ADULT
                        }
                        if (ageClass != expectedAge) {
                            // ----- Validation Rule -----
                            // Seals entered in the previous year must be advanced to the next age class
                            errors += "Seal observed last year as a ${record.ageClass}. Age class should be $expectedAge!"
                        }
                    }

                    else -> {
                        if (ageClass != SealAgeClass.ADULT) {
                            // ----- Validation Rule -----
                            // Seals entered two or more years ago must be Adults
                            errors += "Seal observed two or more years ago. Age class should be Adult!"
                        }
                    }
                }
            }

            return errors
        }

    fun edits(original: Seal?): List<String> {
        if (original == null) return emptyList()

        val edits = mutableListOf<String>()

        if (ageClass != original.ageClass)
            edits.add("ageClass" + " was: ${original.ageClass} now: $ageClass")
        if (comment != original.comment)
            edits.add("comment" + " was: ${original.comment} now: $comment")
        if (condition != original.condition)
            edits.add("condition" + " was: ${original.condition} now: $condition")
        if (isNoTag != original.isNoTag)
            edits.add("isNoTag" + " was: ${original.isNoTag} now: $isNoTag")
        if (sealType == SealType.PRIMARY && numRelatives < original.numRelatives) // only the primary seal can have a change in relatives
            edits.add(
                "numRelatives decreased" + " was: ${original.numRelatives} now: $numRelatives"
            )
        if (sealType == SealType.PRIMARY && numRelatives > original.numRelatives) // only the primary seal can have a change in relatives
            edits.add(
                "numRelatives increased" + " was: ${original.numRelatives} now: $numRelatives"
            )
        if (numTags != original.numTags)
            edits.add("numTags" + " was: ${original.numTags} now: $numTags")
        if (pupPeed != original.pupPeed)
            edits.add("pupPeed" + " was: ${original.pupPeed} now: $pupPeed")
        if (reasonForRetag != original.reasonForRetag)
            edits.add("reasonForRetag" + " was: ${original.reasonForRetag} now: $reasonForRetag")
        if (sex != original.sex)
            edits.add("sex" + " was: ${original.sex} now: $sex")
        if (tagEventType != original.tagEventType)
            edits.add("tagEventType" + " was: ${original.tagEventType} now: $tagEventType")
        if (tagNumber != original.tagNumber || tagAlpha != original.tagAlpha)
            edits.add("tagID" + " was: ${original.tagNumber}${original.tagAlpha} now: $tagNumber$tagAlpha")
        if (oldTagNumber != original.oldTagNumber || oldTagAlpha != original.oldTagAlpha)
            edits.add("oldTagID" + " was: ${original.oldTagNumber}${original.oldTagAlpha} now: $oldTagNumber${oldTagAlpha}")
        if (oldTagMarks != original.oldTagMarks)
            edits.add("oldTagMarks" + " was: ${original.oldTagMarks} now: $oldTagMarks")
        if (tissueTaken != original.tissueTaken || tissue != original.tissue)
            edits.add("tissue" + " was: ${original.tissue} now: $tissue")
        if (weight != original.weight || weightTaken != original.weightTaken)
            edits.add("weight" + " was: ${original.weight} now: $weight")
        if (pupOneRemoved != original.pupOneRemoved)
            edits.add("pupOneRemoved")
        if (pupTwoRemoved != original.pupTwoRemoved)
            edits.add("pupTwoRemoved")

        return edits
    }
}