package weddellseal.markrecap.ui.utils

import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.data.Seal

fun notebookEntryValueSeal(seal : Seal): String {
    val sb = StringBuilder()
    var age = if (seal.age.isNotEmpty()) {
        seal.age[0].toString()
    } else {
        ""
    }

    var sex = if (seal.sex.isNotEmpty()) {
        seal.sex[0].toString()
    } else {
        ""
    }

    val numRels = if (seal.numRelatives > 0) {
        seal.numRelatives.toString()
    } else {
        ""
    }

    var tag = seal.tagIdOne

    var event = if (seal.tagEventType.isNotEmpty()) {
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

    return sb.toString();
}

fun notebookEntryValueObservation(obs : ObservationLogEntry): String {
    val sb = StringBuilder()
    var age = if (obs.ageClass.isNotEmpty()) {
        obs.ageClass[0].toString()
    } else {
        ""
    }

    var sex = if (obs.sex.isNotEmpty()) {
        obs.sex[0].toString()
    } else {
        ""
    }
    val numRels = obs.numRelatives
//    val numRels = if (obs.numRelatives > 0) {
//        obs.numRelatives.toString()
//    } else {
//        ""
//    }

    var tag = obs.tagIDOne
    if (tag == "") {
        tag = obs.tagIDTwo
    }

    var event = if (obs.tagEvent.isNotEmpty()) {
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

    return sb.toString();
}
