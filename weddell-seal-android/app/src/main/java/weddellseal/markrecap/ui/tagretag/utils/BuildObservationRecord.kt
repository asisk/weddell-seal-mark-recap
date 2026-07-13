package weddellseal.markrecap.ui.tagretag.utils

import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.ui.home.ObservationMetadata
import weddellseal.markrecap.ui.utils.getCurrentDateFormatted
import weddellseal.markrecap.ui.utils.getCurrentTimeFormatted

fun buildObservationRecord(
    location: GeoLocation?,
    seal: Seal,
    edits: String,
    relativeOneTag: String,
    relativeTwoTag: String,
    metadata: ObservationMetadata,
): ObservationRecord {
    val metadataCensus = metadata.censusNumber
    val metadataObservers = metadata.getObserversString()
    val metadataColony = metadata.selectedColony?.location ?: ""

    var censusNumber = "0"
    if (metadataCensus != "") {
        censusNumber = metadataCensus
    }

    var observers = "Not Selected"
    if (metadataObservers != "") {
        observers = metadataObservers
    }

    val speNo = if (seal.hasWedCheckMatch) {
        seal.wedCheckMatch?.speNo.toString()
    } else {
        "0"
    }

    var eventType = seal.tagEventType.alpha
    val numberOfTags = seal.numTags.toIntOrNull()
    val isTwoTags = numberOfTags != null && numberOfTags == 2
    var tagIdOne = seal.tagNumber + seal.tagAlpha
    var tagIdTwo = "NoTag"
    var tagOneIndicator = ""
    var tagTwoIndicator = ""
    var oldTagOne = ""
    var oldTagTwo = ""

    if (seal.isNoTag) {
        // if NoTag is selected, both tag columns should be NoTag, and event should be Marked
        eventType = TagEventType.MARKED.alpha
        tagIdOne = "NoTag"

    } else {
        when (seal.tagEventType) { // set the tag columns based on the event type

            TagEventType.MARKED -> {
                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                }
            }

            TagEventType.NEW -> {
                tagOneIndicator = "+"

                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                    tagTwoIndicator = "+"
                }
            }

            TagEventType.RETAG -> {
                tagOneIndicator = "+"

                if (isTwoTags) {
                    tagIdTwo = tagIdOne
                    tagTwoIndicator = "+"
                }

                oldTagOne = seal.oldTagNumber + seal.oldTagAlpha

                // old tag one is only populated when certain reasons for retagging are selected
                if (seal.reasonForRetag == RetagReason.ONE_OF_FOUR || seal.reasonForRetag == RetagReason.TWO_OF_FOUR || seal.reasonForRetag == RetagReason.THREE_OF_FOUR) {
                    // seal is missing a tag
                    oldTagTwo =
                        "NoTag" // the animal is missing a tag, so the second old tag field is marked as "NoTag"
                } else {
                    oldTagTwo =
                        oldTagOne // if the seal is not missing a tag, ie another retag reason is selected, the second value for old tag should match the first value
                }
            }

            TagEventType.UNKNOWN -> {
                // do nothing
            }
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

    // build the comment
    val sb = StringBuilder()
    if (seal.pupPeed) {
        sb.append("pup peed; ")
    }
    if (seal.oldTagMarks) {
        sb.append("old tag marks; ")
    }
    // if the tagEvent is retag and a reason is selected, add the retag reason to the comment
    if (seal.tagEventType == TagEventType.RETAG && (seal.reasonForRetag != RetagReason.NONE && seal.reasonForRetag != RetagReason.UNKNOWN)) {
        sb.append("Reason for Retag: ${seal.reasonForRetag.description}; ")
    }

    sb.append(seal.validationMessage)

    var date = getCurrentDateFormatted()
    var time = getCurrentTimeFormatted()

    // TODO, test when a parent seal has no changes but the pup does not
    if (seal.hasEdits) {
        sb.append("Edited $date at $time: $edits; ") // date that the record was edited + the edits made
        date = metadata.originalDate // for edited records, the original date should be retained
        time = metadata.originalTimestamp // for edited records, the original time should be retained
    }

    val comment = sb.append(seal.comment).toString()

    var flagged = ""
    if (seal.flaggedForReview) {
        flagged = "technician confirmed"
    }

    val log = ObservationRecord(
        // Append-only history: edits create a new row instead of replacing the original row.
        id = 0,
        deviceID = metadata.deviceID,
        season = metadata.currentSeason,
        speno = speNo,
        date = date, // date format: yyyy-MM-dd
        time = time, // time format: hh:mm:ss
        censusID = censusNumber,
        latitude = location?.coordinates?.latitude.toString(),  // example -77.73004, could also be 4 decimal precision
        longitude = location?.coordinates?.longitude.toString(), // example 166.7941, could also be 2 decimal precision
        ageClass = seal.ageClass.alpha,
        sex = seal.sex.alpha,
        numRelatives = seal.numRelatives.label,
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