package weddellseal.markrecap.ui.utils

import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.models.AddObservationLogViewModel

fun buildLogEntry(
    uiState: AddObservationLogViewModel.UiState,
    seal: Seal,
    primarySeal: Seal,
    pupOne: Seal,
    pupTwo: Seal
): ObservationLogEntry {
    var censusNumber = ""
    if (uiState.censusNumber != "Select an option") {
        censusNumber = uiState.censusNumber
    }

    var observers = ""
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

    var numRels = seal.numRelatives

    var tagIdOne = seal.tagOneNumber + seal.tagOneAlpha
    var tagIdTwo = "NoTag"
    var numberOfTags = seal.numTags.toIntOrNull()
    if (numberOfTags != null && numberOfTags == 2) {
        tagIdOne = seal.tagOneNumber + seal.tagOneAlpha
        tagIdTwo = seal.tagTwoNumber + seal.tagTwoAlpha
    }

    var relTwoTagId = ""
    var relOneTagId = ""
    when (seal.name) {
        "primary" -> {
            relOneTagId = pupOne.tagIdOne
            relTwoTagId = pupTwo.tagIdOne
        }

        "pupOne" -> {
            relOneTagId = primarySeal.tagIdOne
            relTwoTagId = pupTwo.tagIdOne
        }

        "pupTwo" -> {
            relOneTagId = primarySeal.tagIdOne
            relTwoTagId = pupOne.tagIdOne
        }
    }

    var eventType = ""
    var tagOneIndicator = ""
    var tagTwoIndicator = ""
    var oldTagOne = ""
    var oldTagTwo = ""
    if (seal.tagEventType.isNotEmpty()) {
        eventType = when (seal.tagEventType) {
            "Marked" -> {
                "M"
            }

            "New" -> {
                tagOneIndicator = "+"
                val number: Int? = seal.numTags.toIntOrNull()
                if (number != null && number > 1) { // this is the definition for two tags
                    tagTwoIndicator = "+"
                }
                "N"
            }

            "Retag" -> {
                oldTagOne = seal.oldTagIdOne
                oldTagTwo = seal.oldTagIdTwo
                tagOneIndicator = "+"
                val number: Int? = seal.numTags.toIntOrNull()
                if (number != null && number > 1) { // this is the definition for two tags
                    tagTwoIndicator = "+"
                }
                "R2"
            }

            else -> {
                ""
            }
        }
    }

    if (seal.isNoTag) {
        eventType = "M"
    }

    var reasonForRetag = ""
    if (seal.reasonForRetag != "") {
        reasonForRetag = seal.reasonForRetag
    }

    var condition = ""
    if (seal.condition != "" && seal.condition != "None") {
        condition = seal.condition.last().toString()
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
    if (seal.reasonNotValid != "") {
        sb.append(seal.reasonNotValid)
    }
    var comment = sb.append(seal.comment).toString()

    var tissue = ""
    if (seal.tissueTaken) {
        tissue = "Tissue"
    }

    var flagged = ""
    if (seal.flaggedForReview) {
        flagged = "C"
    }

    val log = ObservationLogEntry(
        // passing zero, but Room entity will autopopulate the id
        id = 0,
        deviceID = uiState.deviceID,
        season = uiState.season,
        speno = seal.speNo.toString(),
        date = uiState.yearMonthDay, // date format: yyyy-MM-dd
        time = uiState.time, // time format: hh:mm:ss
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
        relativeTagIDOne = relOneTagId,
        relativeTagIDTwo = relTwoTagId,
        sealCondition = condition,
        observerInitials = observers,
        flaggedEntry = flagged,
        tagEvent = eventType,
        reasonForRetag = reasonForRetag,
        weight = pupWeight,
        tissueSampled = tissue,
        comments = comment,
        colony = uiState.colonyLocation,
    )
    return log
}