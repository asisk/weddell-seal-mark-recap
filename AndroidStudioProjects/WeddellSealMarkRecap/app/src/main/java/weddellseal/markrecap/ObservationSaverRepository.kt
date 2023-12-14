package weddellseal.markrecap

/*
Provide access to observations database and csv files.
*/

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import weddellseal.markrecap.database.AppDatabase
import weddellseal.markrecap.database.ObservationLogEntry
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter

const val CSV_FILE_PATH = "./result.csv"
class ObservationSaverRepository(context: Context, private val contentResolver: ContentResolver) {
    private val db = AppDatabase.getDatabase(context)
    var _observations = mutableListOf<ObservationLogEntry>()
    fun getObservations() = _observations.toList()

    /**
     * Fetch a list of Observations from the database.
     * Returns a LiveData-wrapped List of Observations.
     */
    val observations: LiveData<List<ObservationLogEntry>> = liveData<List<ObservationLogEntry>> {
        // Observe observations from the database (just like a normal LiveData + Room return)
        val observationsLiveData = db.observationDao().loadAllObservations()

        // Map the LiveData, applying the sort criteria
        emitSource(observationsLiveData)
    }

    private val obsFolder = File(context.filesDir, "observations").also { it.mkdir() }
    private fun generateFileName() = "${System.currentTimeMillis()}.csv"
    fun generateObservationLogFile() = File(obsFolder, generateFileName())

//    suspend fun writeDataToCSV(uri: Uri) {
//        try {
//            val data: List<ObservationLogEntry> = withContext(Dispatchers.IO) {
//                selectAllObservationsfromDB() // Assuming this function returns a list of observations
//            }
//
//            val obsArr: MutableList<Array<String>> = ArrayList()
//
//            for (obs in data) {
//                val obsFields = arrayOf(
//                    obs?.date ?: "",
//                    obs?.currentLocation ?: "",
//                    obs?.lastKnownLocation ?: ""
//                )
//                obsArr.add(obsFields)
//            }
//            val csvLoc = uri.path
//            CSVUtils().writeDataAtOnce(generateObservationLogFile(), obsArr)
//
//        } catch (ioe: IOException) {
//            ioe.printStackTrace()
//            // Handle any exceptions
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    suspend fun writeDataToFile(uri: Uri) {
        try {
            val data: List<ObservationLogEntry> = withContext(Dispatchers.IO) {
                selectAllObservationsfromDB() // Assuming this function returns a list of observations
            }

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                val csvWriter = CSVWriter(writer)

                // Define the header row if needed
//                val header = arrayOf("Date", "Current Location", "Last Known Location")
//                csvWriter.writeNext(header)

                for (obs in data) {
                    // Create a String array for the data
                    val obsFields = arrayOf(
                        obs?.date ?: "",
                        obs?.currentLocation ?: "",
                        obs?.lastKnownLocation ?: ""
                    )
                    csvWriter.writeNext(obsFields)
                }

                csvWriter.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle any exceptions
        }
    }


//    suspend fun saveObservations(file: File): Boolean {
//        return withContext(Dispatchers.IO) {
//            var success = false
//            try {
//                _observations = selectAllObservationsfromDB()
//                if (_observations.isNotEmpty()) {
//                    success = writeObservationsToCSV(file,_observations)
////                    success = false
//                } else {
////                    success = writeObservationsToCSV(uriForCSVWrite,_observations)
//                }
//            } catch (e : Exception) {
//                println(e)
////                success = false
//            }
//            success
//        }
//    }

    // Function to write data to the selected file
//    fun writeToFile(uri: Uri, contentResolver: ContentResolver, data: String): Boolean {
//        var success = false
//        try {
//            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
//            if (outputStream != null) {
//                outputStream.write(data.toByteArray())
//                outputStream.close()
//                success = true
//            }
//        } catch (e: Exception) {
//            success = false
//            e.printStackTrace()
//        }
//        return success
//    }
    fun writeObservationsToCSV(file: File, obsList: List<ObservationLogEntry>) : Boolean {
        val data: MutableList<Array<String>> = ArrayList()

        for (obs in obsList) {
            val obsFields = "${obs?.date ?: ""},${obs?.currentLocation ?: ""},${obs?.lastKnownLocation ?: ""}"
            data.add(arrayOf(obsFields))
        }

        return try {
            CSVUtils().writeDataAtOnce(file, data)
            true
        }catch (e : Exception){
            false
        }
    }

    fun isEmpty() = _observations.isEmpty()

    fun canAddObservation() = true

    suspend fun addObservation(log: ObservationLogEntry) {
        db.observationDao().insert(log)
    }

    private fun selectAllObservationsfromDB() : MutableList<ObservationLogEntry> {
        val logs : List<ObservationLogEntry?> = db.observationDao().getObservationsForCSVWrite()
        var list = mutableListOf<ObservationLogEntry>()
        for (log in logs) {
            if (log != null) {
                list.add(log)
            }
        }
        return list
    }

}
