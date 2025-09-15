package weddellseal.markrecap.frameworks.room.observations

/*
Provide access to observations database and csv files.
*/

import android.util.Log
import com.opencsv.CSVWriter
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import java.io.OutputStreamWriter

class ObservationRepository(private val observationDao: ObservationDao) {

    val currentObservationsDescByID: Flow<List<ObservationRecord>> = observationDao.getCurrentObservationsDescByID()
    val currentObservationsByID: Flow<List<ObservationRecord>> = observationDao.getCurrentObservationsForSeasonByID()

    val allObservationsDescByID: Flow<List<ObservationRecord>> = observationDao.getAllObservationsForSeasonDescByID()


    // Write all observations to a CSV file
    fun writeDataToStream(outputStream: OutputStream, observations: List<ObservationRecord>) {
        OutputStreamWriter(outputStream).use { writer ->
            val csvWriter = CSVWriter(writer)

            for (obs in observations) {
                // Create a String array for the data
                val obsFields = arrayOf(
                    obs.deviceID,
                    obs.season,
                    obs.speno,
                    obs.date,
                    obs.time,
                    obs.censusID,
                    obs.latitude,
                    obs.longitude,
                    obs.ageClass,
                    obs.sex,
                    obs.numRelatives,
                    obs.oldTagIDOne,
                    obs.oldTagIDTwo,
                    obs.tagIDOne,
                    obs.tagOneIndicator,
                    obs.tagIDTwo,
                    obs.tagTwoIndicator,
                    obs.relativeTagIDOne,
                    obs.relativeTagIDTwo,
                    obs.sealCondition,
                    obs.observerInitials,
                    obs.flaggedEntry,
                    obs.tagEvent,
                    obs.weight,
                    obs.tissueSampled,
                    obs.comments,
                    obs.colony
                )
                csvWriter.writeNext(obsFields)
            }
            Log.d("writeDataToStream", "All records written")
            csvWriter.close()
        }
    }

    suspend fun writeObservation(log: ObservationRecord) {
        observationDao.upsert(log)
    }

    suspend fun deleteObservation(id: Int) {
        observationDao.delete(id)
    }

    suspend fun softDeleteAllObservations() {
        observationDao.softDeleteObservations()
    }

    suspend fun deleteAll() {
        observationDao.deleteAll()
    }

}
