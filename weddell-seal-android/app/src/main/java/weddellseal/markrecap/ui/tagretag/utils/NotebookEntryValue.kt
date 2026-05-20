package weddellseal.markrecap.ui.tagretag.utils

import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord

// function used to display the notebook string on the seal card in the observations view
fun notebookEntryValueSeal(seal: Seal): String {
    val sb = StringBuilder()
    val age = if (seal.ageClass != SealAgeClass.UNKNOWN) {
        seal.ageClass.alpha
    } else {
        ""
    }

    val numberOfTags = seal.numTags.toIntOrNull()
    val isTwoTags = numberOfTags != null && numberOfTags == 2
    var tag = seal.tagNumber + seal.tagAlpha
    if (isTwoTags) {
        tag = seal.tagNumber + seal.tagAlpha + seal.tagAlpha
    }

    sb.append(age)
    sb.append(seal.sex.alpha)
    sb.append(seal.numRelatives.label)
    sb.append("  ")
    sb.append(tag)
    sb.append("  ")
    sb.append(seal.tagEventType.alpha)

    if (seal.numTags == "NoTag") {
        sb.append("  ")
        sb.append(seal.numTags)
    }

    // display condition only for Pups
    if (seal.ageClass == SealAgeClass.PUP && seal.condition.code != "") {
        sb.append("  ")
        sb.append("C=")
        sb.append(seal.condition.code)
    }

    if (seal.pupPeed) {
        sb.append("  ")
        sb.append("peed")
    }

    return sb.toString()
}

// function used to display the notebook string in the recent observations view
fun notebookEntryValueObservation(obs: ObservationRecord): String {
    val sb = StringBuilder()

    val numRels = obs.numRelatives

    var tag = obs.tagIDOne
    if (tag == "") {
        tag = obs.tagIDTwo
    }

    sb.append(SealAgeClass.fromAlpha(obs.ageClass).alpha)
    sb.append(SealSex.fromAlpha(obs.sex).alpha)
    sb.append(numRels)
    sb.append("  ")
    sb.append(tag)
    sb.append("  ")
    sb.append(TagEventType.fromAlpha(obs.tagEvent).alpha)

    return sb.toString()
}