package weddellseal.markrecap.ui.recentobservations

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
