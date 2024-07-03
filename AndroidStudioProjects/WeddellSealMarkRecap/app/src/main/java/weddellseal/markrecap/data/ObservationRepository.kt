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

const val CSV_FILE_PATH = "./result.csv"

class ObservationRepository(private val observationDao: ObservationDao) {

    /**
     * Fetch a list of Observations from the database for the Recent Observations Screen.
     * Returns a LiveData-wrapped List of Observations.
     */
    val observations: LiveData<List<ObservationLogEntry>> = liveData<List<ObservationLogEntry>> {
        // Observe observations from the database (just like a normal LiveData + Room return)
        val observationsLiveData = observationDao.loadAllObservations()

        // Map the LiveData, applying the sort criteria
        emitSource(observationsLiveData)
    }

    private fun generateFileName() = "${System.currentTimeMillis()}.csv"

    suspend fun writeDataToFile(uri: Uri, contentResolver: ContentResolver) {
        try {
            val data: List<ObservationLogEntry> = withContext(Dispatchers.IO) {
                selectAllObservationsfromDB() // Assuming this function returns a list of observations
            }

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                val csvWriter = CSVWriter(writer)

                // Define the header row
//                val header = arrayOf("Date", "Current Location", "Last Known Location")
//                csvWriter.writeNext(header)

                for (obs in data) {
                    // Create a String array for the data
                    val obsFields = arrayOf(
                        obs?.deviceID ?: "",
                        obs?.season ?: "",
                        obs?.speno ?: "",
                        obs?.date ?: "",
                        obs?.time ?: "",
                        obs?.censusID ?: "",
                        obs?.latitude  ?: "",
                        obs?.longitude ?: "",
                        obs?.ageClass ?: "",
                        obs?.sex ?: "",
                        obs?.numRelatives ?: "",
                        obs?.oldTagIDOne ?: "",
                        obs?.oldTagOneCondition ?: "",
                        obs?.oldTagIDTwo ?: "",
                        obs?.oldTagTwoCondition ?: "",
                        obs?.tagIDOne ?: "",
                        obs?.tagOneIndicator ?: "",
                        obs?.tagIDTwo ?: "",
                        obs?.tagTwoIndicator ?: "",
                        obs?.relativeTagIDOne ?: "",
                        obs?.relativeTagIDTwo ?: "",
                        obs?.sealCondition ?: "",
                        obs?.observerInitials ?: "",
                        obs?.flaggedEntry ?: "",
                        obs?.tagEvent ?: "",
                        obs?.tissueSampled ?: "",
                        obs?.comments ?: "",
                        )
                    csvWriter.writeNext(obsFields)
                }

                csvWriter.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    fun writeObservationsToCSV(file: File, obsList: List<ObservationLogEntry>) : Boolean {
//        val data: MutableList<Array<String>> = ArrayList()
//
//        for (obs in obsList) {
//            val obsFields = "${obs?.date ?: ""},${obs?.currentLocation ?: ""},${obs?.lastKnownLocation ?: ""}"
//            data.add(arrayOf(obsFields))
//        }
//
//        return try {
//            CSVUtils().writeDataAtOnce(file, data)
//            true
//        } catch (e : Exception){
//            false
//        }
//    }

//    fun isEmpty() = _observations.isEmpty()

    fun canAddObservation() = true

    suspend fun addObservation(log: ObservationLogEntry) {
        observationDao.insert(log)
    }

    private fun selectAllObservationsfromDB() : MutableList<ObservationLogEntry> {
        val logs : List<ObservationLogEntry?> = observationDao.getObservationsForCSVWrite()
        var list = mutableListOf<ObservationLogEntry>()
        for (log in logs) {
            if (log != null) {
                list.add(log)
            }
        }
        return list
    }

    fun canWriteStudyAreas(): Boolean {
        //TODO, implement for the study area csv
        return true
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var INSTANCE: ObservationRepository? = null

        fun getInstance(observationDao: ObservationDao) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ObservationRepository(observationDao).also { INSTANCE = it }
            }
    }


}
