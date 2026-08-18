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
 * Pups recorded with a mom (`numRelatives > 0`) are shown on the mom's row, not as
 * standalone entries. Relative-tag matching requires a pup row and ignores `NoTag`
 * placeholders so dummy tag `0000D` cannot attach the mom (or another adult) as a pup.
 */
fun observationsToDisplay(current: List<ObservationRecord>): List<DisplayObservation> =
    current
        .filterNot { obs ->
            val numRelatives = obs.numRelatives.toIntOrNull() ?: 0
            obs.ageClass == SealAgeClass.PUP.alpha && numRelatives > 0
        }
        .map { obs ->
            val numRelatives = obs.numRelatives.toIntOrNull() ?: 0
            if (numRelatives > 0 &&
                obs.ageClass == SealAgeClass.ADULT.alpha &&
                obs.sex == SealSex.FEMALE.alpha
            ) {
                DisplayObservation.WithPups(
                    primarySeal = obs,
                    pupOne = findLinkedPup(current, obs.relativeTagIDOne, obs),
                    pupTwo = findLinkedPup(current, obs.relativeTagIDTwo, obs),
                )
            } else {
                DisplayObservation.Standalone(obs)
            }
        }

private fun findLinkedPup(
    current: List<ObservationRecord>,
    relativeId: String,
    parent: ObservationRecord,
): ObservationRecord? {
    if (relativeId.isBlank() || relativeId.equals("NoTag", ignoreCase = true)) return null
    return current.find { candidate ->
        candidate.id != parent.id &&
            candidate.ageClass == SealAgeClass.PUP.alpha &&
            candidate.tagIDOne.equals(relativeId, ignoreCase = true)
    }
}
