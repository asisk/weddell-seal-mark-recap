package weddellseal.markrecap.ui.utils

import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.models.AddObservationLogViewModel

fun buildLogEntry(
    uiState: AddObservationLogViewModel.UiState,
    seal: Seal,
    relativeOneTag: String,
    relativeTwoTag: String,
): ObservationLogEntry {
    var censusNumber = "0"
    if (uiState.censusNumber != "Select an option") {
        censusNumber = uiState.censusNumber
    }

    var observers = "Not Selected"
    if (uiState.observerInitials != "Select an option") {
        observers = uiState.observerInitials
    }

    var ageClass = ""
    if (seal.age != "") {
        ageClass = seal.age[0].toString()
    }

    var sex = ""
    if (seal.sex != "") {
        sex = seal.sex[0].toString()
    }

    val numRels = seal.numRelatives

    var eventType = ""
    val numberOfTags = seal.numTags.toIntOrNull()
    val isTwoTags = numberOfTags != null && numberOfTags == 2
    var tagIdOne = seal.tagNumber + seal.tagAlpha
    var tagIdTwo = "NoTag"
    var tagOneIndicator = ""
    var tagTwoIndicator = ""
    var oldTagOne = ""
    var oldTagTwo = ""
    // if NoTag is selected, both tag columns should be NoTag, and event should be Marked
    if (seal.isNoTag) {
        eventType = "M"
        tagIdOne = "NoTag"
    } else if (seal.tagEventType.isNotEmpty()) {
        eventType = when (seal.tagEventType) {
            "Marked" -> {
                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                }

                // value for the event type of Marked
                "M"
            }

            "New" -> {
                tagOneIndicator = "+"

                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                    tagTwoIndicator = "+"
                }

                // value for the event type of New
                "N"
            }

            "Retag" -> {
                tagOneIndicator = "+"

                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                    tagTwoIndicator = "+"
                }

                oldTagOne = seal.oldTagId

                // old tag one is only populated when certain reasons for retagging are selected
                if (seal.reasonForRetag == "1 of 4" || seal.reasonForRetag == "2 of 4" || seal.reasonForRetag == "3 of 4") { // seal is missing a tag
                    oldTagTwo = "NoTag" // the animal is missing a tag, so the second old tag field is marked as "NoTag"
                } else {
                    oldTagTwo = oldTagOne // if the seal is not missing a tag, ie another retag reason is selected, the second value for old tag should match the first value
                }

                // value for the event type of Retag
                "R2"
            }

            else -> {""}
        }
    }

    var condition = ""
    if (seal.condition != "" && seal.condition != "None" && seal.condition != "Select an option") {
        condition = seal.condition.last().toString()
    }

    var tissue = ""
    if (seal.tissueTaken) {
        tissue = "Tissue"
    }

    var pupWeight = ""
    if (seal.weight > 0) {
        pupWeight = seal.weight.toString()
    }

    val sb = StringBuilder()
    if (seal.pupPeed) {
        sb.append("pup peed; ")
    }
    if (seal.oldTagMarks) {
        sb.append("old tag marks; ")
    }
    if (seal.tagEventType == "Retag" && seal.reasonForRetag != "") {
        sb.append("reason for retag: ${seal.reasonForRetag}; ")
    }
    if (seal.reasonNotValid != "") {
        sb.append(seal.reasonNotValid)
    }
    val comment = sb.append(seal.comment).toString()

    var flagged = ""
    if (seal.flaggedForReview) {
        flagged = "C"
    }

    val log = ObservationLogEntry(
        id = 0, // passing zero, but Room entity will autopopulate the id
        deviceID = uiState.deviceID,
        season = uiState.season,
        speno = seal.speNo.toString(),
        date = getCurrentDateFormatted(), // date format: yyyy-MM-dd
        time = getCurrentTimeFormatted(), // time format: hh:mm:ss
        censusID = censusNumber,
        latitude = uiState.latitude,  // example -77.73004, could also be 4 decimal precision
        longitude = uiState.longitude, // example 166.7941, could also be 2 decimal precision
        ageClass = ageClass,
        sex = sex,
        numRelatives = numRels,
        oldTagIDOne = oldTagOne,
        oldTagIDTwo = oldTagTwo,
        tagIDOne = tagIdOne,
        tagOneIndicator = tagOneIndicator,
        tagIDTwo = tagIdTwo,
        tagTwoIndicator = tagTwoIndicator,
        relativeTagIDOne = relativeOneTag,
        relativeTagIDTwo = relativeTwoTag,
        sealCondition = condition,
        observerInitials = observers,
        flaggedEntry = flagged,
        tagEvent = eventType,
        weight = pupWeight,
        tissueSampled = tissue,
        comments = comment,
        colony = uiState.colonyLocation,
    )
    return log
}