package weddellseal.markrecap.ui

/*
 *
 *
 */

import weddellseal.markrecap.database.ObservationLogEntry
import java.text.SimpleDateFormat
import java.util.Locale

data class ObservationLog(
    val date: String,
    val currentLocation: String,
    val lastKnownLocation: String,
) {
    val timeInMillis = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!.time
    override fun toString(): String {
        return "$date, $currentLocation, $lastKnownLocation"
    }
    fun toLogEntry(): ObservationLogEntry {
        return ObservationLogEntry(
            id = 1,
            date = date,
            currentLocation = currentLocation,
            lastKnownLocation = lastKnownLocation,
        )
    }

    companion object {
        fun fromLogEntry(observationLogEntry: ObservationLogEntry): ObservationLog {
            return ObservationLog(
                date = observationLogEntry.date,
                currentLocation = observationLogEntry.currentLocation,
                lastKnownLocation = observationLogEntry.lastKnownLocation
            )
        }
    }
}
