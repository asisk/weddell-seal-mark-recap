package weddellseal.markrecap.data

/*
Provide access to observations database and csv files.
*/

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter

class ObservationRepository(private val observationDao: ObservationDao) {

    /**
     * Fetch a list of Observations from the database for the Recent Observations Screen.
     * Returns a LiveData-wrapped List of Observations.
     */
    val observations: LiveData<List<ObservationLogEntry>> = liveData {
        // Observe active observations from the database (just like a normal LiveData + Room return)
        val observationsLiveData = observationDao.loadCurrentObservations()

        // Map the LiveData, applying the sort criteria
        emitSource(observationsLiveData)
    }

    suspend  fun getCurrentObservations(): List<ObservationLogEntry> {
        val observations: List<ObservationLogEntry> = observationDao.getCurrentObservations()
        return observations
    }

    suspend fun getAllObservations(): List<ObservationLogEntry> {
        val observations: List<ObservationLogEntry> = observationDao.getObservationsForSeasonView()
        return observations
    }

    suspend fun writeDataToFile(uri: Uri, contentResolver: ContentResolver, observations: List<ObservationLogEntry>) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch all observations from the database that have not been deleted
                val data: List<ObservationLogEntry> = observations

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        val csvWriter = CSVWriter(writer)

                        println("Number of records: ${data.size}")

                        for (obs in data) {
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
                        println("All records written")

                        csvWriter.close()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addObservation(log: ObservationLogEntry) {
        observationDao.insert(log)
    }

    suspend fun softDeleteAllObservations() {
        observationDao.softDeleteObservations()
    }

    suspend fun updateObservationEntry(log: ObservationLogEntry) {
        // Example of updating an existing entry with ID 1
        observationDao.updateObservationLogEntry(log)
    }

}
