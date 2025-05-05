package weddellseal.markrecap.frameworks.room.observations

/*
Provide access to observations database and csv files.
*/

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.opencsv.CSVWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

class ObservationRepository(private val observationDao: ObservationDao) {

    /**
     * Fetch a list of Observations from the database for the Recent Observations Screen.
     * Returns a LiveData-wrapped List of Current Observations.
     */
    val currentObservations: LiveData<List<ObservationLogEntry>> = liveData {
        // Observe active observations from the database (just like a normal LiveData + Room return)
        val observationsLiveData = observationDao.getCurrentObservationsOrdered()

        // Map the LiveData, applying the sort criteria
        emitSource(observationsLiveData)
    }

    val allObservations: LiveData<List<ObservationLogEntry>> = liveData {
        val allObservationsLiveData = observationDao.getAllObservationsOrdered()

        emitSource(allObservationsLiveData)
    }

    suspend fun getCurrentObservations(): List<ObservationLogEntry> {
        val observations: List<ObservationLogEntry> = observationDao.getCurrentObservations()
        return observations
    }

    suspend fun getAllObservations(): List<ObservationLogEntry> {
        val observations: List<ObservationLogEntry> = observationDao.getObservationsForSeasonView()
        return observations
    }

    // TODO, this is writing BIN files on repeated exports...figure out what's happening here
    fun writeDataToStream(outputStream: OutputStream, observations: List<ObservationLogEntry>) {
        OutputStreamWriter(outputStream).use { writer ->
            val csvWriter = CSVWriter(writer)

//            writer.appendLine("ID,Date,Notes") // Customize your header
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

//    suspend fun writeDataToFile(
//        uri: Uri,
//        contentResolver: ContentResolver,
//        observations: List<ObservationLogEntry>
//    ) {
//        withContext(Dispatchers.IO) {
//            try {
//                // Fetch all observations from the database that have not been deleted
//                val data: List<ObservationLogEntry> = observations
//
//                contentResolver.openOutputStream(uri)?.use { outputStream ->
//                    OutputStreamWriter(outputStream).use { writer ->
//                        val csvWriter = CSVWriter(writer)
//
//                        println("Number of records: ${data.size}")
//
//                        for (obs in data) {
//                            // Create a String array for the data
//                            val obsFields = arrayOf(
//                                obs.deviceID,
//                                obs.season,
//                                obs.speno,
//                                obs.date,
//                                obs.time,
//                                obs.censusID,
//                                obs.latitude,
//                                obs.longitude,
//                                obs.ageClass,
//                                obs.sex,
//                                obs.numRelatives,
//                                obs.oldTagIDOne,
//                                obs.oldTagIDTwo,
//                                obs.tagIDOne,
//                                obs.tagOneIndicator,
//                                obs.tagIDTwo,
//                                obs.tagTwoIndicator,
//                                obs.relativeTagIDOne,
//                                obs.relativeTagIDTwo,
//                                obs.sealCondition,
//                                obs.observerInitials,
//                                obs.flaggedEntry,
//                                obs.tagEvent,
//                                obs.weight,
//                                obs.tissueSampled,
//                                obs.comments,
//                                obs.colony
//                            )
//                            csvWriter.writeNext(obsFields)
//                        }
//                        println("All records written")
//
//                        csvWriter.close()
//                    }
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }

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
