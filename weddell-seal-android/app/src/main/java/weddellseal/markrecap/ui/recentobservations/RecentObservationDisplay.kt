package weddellseal.markrecap.ui.recentobservations

import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord

sealed class DisplayObservation {
    data class WithPups(
        val primarySeal: ObservationRecord,
        val pupOne: ObservationRecord?,
        val pupTwo: ObservationRecord?
    ) : DisplayObservation()

    data class Standalone(
        val primarySeal: ObservationRecord
    ) : DisplayObservation()
}

/**
 * Groups current observation rows for the recent-observations list.
 *
 * A mom (adult female with relatives) is shown as one row with up to two pups
 * attached by reciprocal relative tags: the mom's relative tag matches the pup's
 * tag, and the pup's relative tag points back at the mom. `NoTag` placeholders
 * are ignored. A pup is used at most once, so two dummy `0000D` pups of the same
 * tagged mom can fill both slots.
 *
 * Pups that do not match a mom stay on their own row. Only attached pups are
 * omitted from the top-level list.
 */
fun observationsToDisplay(current: List<ObservationRecord>): List<DisplayObservation> {
    val attachedPupIds = mutableSetOf<Int>()
    val pupsByMomId = mutableMapOf<Int, Pair<ObservationRecord?, ObservationRecord?>>()

    for (obs in current) {
        if (!obs.isMomWithRelatives()) continue
        val pupOne = findLinkedPup(current, obs.relativeTagIDOne, obs, attachedPupIds)
        if (pupOne != null) attachedPupIds += pupOne.id
        val pupTwo = findLinkedPup(current, obs.relativeTagIDTwo, obs, attachedPupIds)
        if (pupTwo != null) attachedPupIds += pupTwo.id
        pupsByMomId[obs.id] = pupOne to pupTwo
    }

    return current.mapNotNull { obs ->
        when {
            obs.id in attachedPupIds -> null
            obs.isMomWithRelatives() -> {
                val (pupOne, pupTwo) = pupsByMomId.getValue(obs.id)
                DisplayObservation.WithPups(obs, pupOne, pupTwo)
            }
            else -> DisplayObservation.Standalone(obs)
        }
    }
}

private fun ObservationRecord.isMomWithRelatives(): Boolean {
    val relativeCount = numRelatives.toIntOrNull() ?: 0
    return relativeCount > 0 &&
        ageClass == SealAgeClass.ADULT.alpha &&
        sex == SealSex.FEMALE.alpha
}

private fun findLinkedPup(
    current: List<ObservationRecord>,
    relativeId: String,
    parent: ObservationRecord,
    excludeIds: Set<Int>,
): ObservationRecord? {
    if (relativeId.isBlank() || relativeId.equals("NoTag", ignoreCase = true)) return null
    return current.find { candidate ->
        candidate.id != parent.id &&
            candidate.id !in excludeIds &&
            candidate.ageClass == SealAgeClass.PUP.alpha &&
            candidate.tagIDOne.equals(relativeId, ignoreCase = true) &&
            candidate.relativePointsTo(parent.tagIDOne)
    }
}

private fun ObservationRecord.relativePointsTo(parentTag: String): Boolean {
    if (parentTag.isBlank() || parentTag.equals("NoTag", ignoreCase = true)) return false
    return relativeTagIDOne.equals(parentTag, ignoreCase = true) ||
        relativeTagIDTwo.equals(parentTag, ignoreCase = true)
}
