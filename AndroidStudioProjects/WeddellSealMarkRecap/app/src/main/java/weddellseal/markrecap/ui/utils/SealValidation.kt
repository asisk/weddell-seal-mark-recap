package weddellseal.markrecap.ui.utils

import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.data.WedCheckSeal

fun sealValidation(
    seal: Seal,
    currentYear: Int,
    wedCheckSeal: WedCheckSeal?
): Pair<Boolean, String> {
    var sealValid = true // assume the seal entry is valid until proven false
    val validationFailureReasons = StringBuilder() // validation errors will be added here

    if (seal.tagEventType == "New" && wedCheckSeal != null) {
        sealValid = false // new event types CANNOT have a WedCheck record
        validationFailureReasons.append(
            "Tag already used! Recheck all fields before saving!\n" +
                    "If you choose to save this entry, please take a photo and add a comment."
        )

    } else if (seal.tagEventType == "Retag" || seal.tagEventType == "Marked") {
        if (wedCheckSeal == null) {
            sealValid = false // marked and retagged seals MUST have a WedCheck record
            validationFailureReasons.append(
                "Seal not in database!\n" +
                        "If you choose to save this entry, please take a photo and add a comment."
            )

        } else { // check the entered field values against those in the WedCheck record
            // sex must match
            if (seal.sex != wedCheckSeal.sex) {
                sealValid = false
                validationFailureReasons.append("\n " + "Sex doesn't match")
            }

            // number of tags must match
            if (seal.numTags != wedCheckSeal.numTags) {
                sealValid = false
                validationFailureReasons.append("\n " + "Number of tags doesn't match")
            }

            // dead seals should not have new observations
            if (wedCheckSeal.condition == "0") {
                sealValid = false
                validationFailureReasons.append("\n " + "Seal last seen dead")
            }

            // new observations for seals seen more than ten years ago are unlikely
            if (wedCheckSeal.lastSeenSeason < (currentYear - 10)) { // TODO, need to test this with null and empty values
                sealValid = false
                validationFailureReasons.append("\n " + "Seal last seen more than ten years ago")
            }

            when (wedCheckSeal.lastSeenSeason) {
                currentYear -> { // seal last seen this year
                    // Age class CANNOT change for seals seen twice in a season
                    if (seal.age != wedCheckSeal.age) {
                        sealValid = false
                        validationFailureReasons.append("\n Seal last observed this year. Age class can't change!")
                    }
                }

                currentYear - 1 -> { // seal last seen last year
                    // Age class must advance for seals seen last year
                    val expectedAge = when (wedCheckSeal.age) {
                        "Pup" -> "Yearling"
                        "Yearling" -> "Adult"
                        else -> "Adult"
                    }

                    if (seal.age != expectedAge) {
                        sealValid = false
                        validationFailureReasons.append("\n Seal observed last year as a ${wedCheckSeal.age}. Age class should be $expectedAge!")
                    }
                }

                currentYear - 2 -> { // seal last seen 2 or more years ago
                    // Age class must be Adult if seal was observed two or more years ago
                    if (seal.age != "Adult") {
                        sealValid = false
                        validationFailureReasons.append("\n Seal observed two years ago. Age class should be adult!")
                    }
                }

                else -> {
                    if (seal.age != wedCheckSeal.age) {
                        sealValid = false
                        validationFailureReasons.append("\n Age class should match.")
                    }
                }
            }
        }
    }

    return Pair(sealValid, validationFailureReasons.toString())
}