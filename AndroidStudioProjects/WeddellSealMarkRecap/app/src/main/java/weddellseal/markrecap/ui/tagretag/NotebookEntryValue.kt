package weddellseal.markrecap.ui.tagretag

import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.frameworks.room.observations.Seal

// function used to display the notebook string on the seal card in the observations view
fun notebookEntryValueSeal(seal: Seal): String {
    val sb = StringBuilder()
    val age = if (seal.age.isNotEmpty()) {
        seal.age[0].toString()
    } else {
        ""
    }

    val sex = if (seal.sex.isNotEmpty()) {
        seal.sex[0].toString()
    } else {
        ""
    }

    var numRels = ""
    if (seal.numRelatives != "") {
        numRels = seal.numRelatives
    }

    val numberOfTags = seal.numTags.toIntOrNull()
    val isTwoTags = numberOfTags != null && numberOfTags == 2
    var tag = seal.tagNumber + seal.tagAlpha
    if (isTwoTags) {
        tag = seal.tagNumber + seal.tagAlpha + seal.tagAlpha
    }

    val event = if (seal.tagEventType.isNotEmpty()) {
        seal.tagEventType[0]
    } else {
        ""
    }

    sb.append(age)
    sb.append(sex)
    sb.append(numRels)
    sb.append("  ")
    sb.append(tag)
    sb.append("  ")
    sb.append(event)

    if (seal.numTags == "NoTag") {
        sb.append("  ")
        sb.append(seal.numTags)
    }

    if (seal.pupPeed) {
        sb.append("  ")
        sb.append("peed")
    }

    return sb.toString()
}

// function used to display the notebook string in the recent observations view
fun notebookEntryValueObservation(obs: ObservationLogEntry): String {
    val sb = StringBuilder()
    val age = if (obs.ageClass.isNotEmpty()) {
        obs.ageClass[0].toString()
    } else {
        ""
    }

    val sex = if (obs.sex.isNotEmpty()) {
        obs.sex[0].toString()
    } else {
        ""
    }
    val numRels = obs.numRelatives

    var tag = obs.tagIDOne
    if (tag == "") {
        tag = obs.tagIDTwo
    }

    val event = if (obs.tagEvent.isNotEmpty()) {
        obs.tagEvent[0]
    } else {
        ""
    }
    sb.append(age)
    sb.append(sex)
    sb.append(numRels)
    sb.append("  ")
    sb.append(tag)
    sb.append("  ")
    sb.append(event)

    return sb.toString()
}