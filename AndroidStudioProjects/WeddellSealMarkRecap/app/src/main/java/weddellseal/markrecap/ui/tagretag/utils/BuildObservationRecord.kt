package weddellseal.markrecap.ui.tagretag.utils

import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.ui.tagretag.TagRetagModel.ObservationMetadata
import weddellseal.markrecap.ui.utils.getCurrentDateFormatted
import weddellseal.markrecap.ui.utils.getCurrentTimeFormatted

fun buildObservationRecord(
    currentLocation: GeoLocation?,
    seal: Seal,
    relativeOneTag: String,
    relativeTwoTag: String,
    metadata: ObservationMetadata,
): ObservationRecord {
    val metadataCensus = metadata.censusNumber
    val metadataObservers = metadata.getObserversString()
    val metadataColony = metadata.selectedColony

    var censusNumber = "0"
    if (metadataCensus != "") {
        censusNumber = metadataCensus
    }

    var observers = "Not Selected"
    if (metadataObservers != "") {
        observers = metadataObservers
    }

    var speNo = if (seal.hasWedCheckMatch) {
        seal.wedCheckMatch?.speNo.toString()
    } else {
        "0"
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

                oldTagOne = seal.oldTagNumber + seal.oldTagAlpha

                // old tag one is only populated when certain reasons for retagging are selected
                if (seal.reasonForRetag == RetagReason.ONE_OF_FOUR || seal.reasonForRetag == RetagReason.TWO_OF_FOUR || seal.reasonForRetag == RetagReason.THREE_OF_FOUR) {
                    // seal is missing a tag
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
    if (seal.tagEventType == "Retag" && (seal.reasonForRetag == RetagReason.NONE || seal.reasonForRetag == RetagReason.UNKNOWN)) {
        sb.append("reason for retag: ${seal.reasonForRetag.description}; ")
    }
    if (seal.validationMessage != "") {
        sb.append(seal.validationMessage)
    }
    val comment = sb.append(seal.comment).toString()

    var flagged = ""
    if (seal.flaggedForReview) {
        flagged = "C"
    }

    val log = ObservationRecord(
        id = 0, // passing zero, but Room entity will auto-populate the id
        deviceID = metadata.deviceID,
        season = metadata.currentSeason,
        speno = speNo,
        date = getCurrentDateFormatted(), // date format: yyyy-MM-dd
        time = getCurrentTimeFormatted(), // time format: hh:mm:ss
        censusID = censusNumber,
        latitude = currentLocation?.coordinates?.latitude.toString(),  // example -77.73004, could also be 4 decimal precision
        longitude = currentLocation?.coordinates?.longitude.toString(), // example 166.7941, could also be 2 decimal precision
        ageClass = seal.ageClass.alpha,
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
        sealCondition = seal.condition.code,
        observerInitials = observers,
        flaggedEntry = flagged,
        tagEvent = eventType,
        weight = pupWeight,
        tissueSampled = tissue,
        comments = comment,
        retagReason = seal.reasonForRetag.description,
        colony = metadataColony,
    )
    return log
}