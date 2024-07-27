package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.WedCheckRecord
import weddellseal.markrecap.data.WedCheckRepository
import weddellseal.markrecap.data.toSeal
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class WedCheckViewModel(
    application: Application,
    private val wedCheckRepo: WedCheckRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    var wedCheckSeal by mutableStateOf(WedCheckSeal())

    var uiState by mutableStateOf(
        UiState(
            hasFileAccess = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )
        private set

    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = true,
        var isSearching: Boolean = false,
        val sealNotFound: Boolean = false,
        val uriForCSVDataLoad: Uri? = null,
        val sealRecordDB: WedCheckRecord? = null,
        val date: String, //TODO, think about the proper date format, should it be UTC?
        val isError: Boolean = false,
    )

    data class WedCheckSeal(
        val age: String = "",
        val ageYears: String = "",
        val comment: String = "",
        val condition: String = "",
        var found: Boolean = false,
        val isWedCheckRecord: Boolean = false,
        val lastSeenSeason: Int = 0,
        val massPups: String = "",
        val name: String = "",
        val numRelatives: Int = 0,
        val numTags: String = "",
        val photoYears: String = "",
        val previousPups: String = "",
        val pupPeed: Boolean = false,
        val sex: String = "",
        val speNo: Int = 0,
        val swimPups: String = "",
        val tagAlpha: String = "",
        val tagEventType: String = "",
        val tagId: String = "",
        val tagNumber: Int = 0,
        val tissueSampled: String = ""
    )

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                uiState = uiState.copy(hasFileAccess = isGranted)
            }

            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    fun findSeal(sealTagID: String) {
        uiState = uiState.copy(isSearching = true)

        // launch the search for a seal on a separate coroutine
        viewModelScope.launch {

            // Switch to the IO dispatcher for database operation
            val seal: WedCheckRecord = withContext(Dispatchers.IO) {
                wedCheckRepo.findSeal(sealTagID)
            }
            if (seal != null) {
                uiState =
                    uiState.copy(sealRecordDB = seal, isSearching = false, sealNotFound = false)
                wedCheckSeal = seal.toSeal()
            } else {
                uiState =
                    uiState.copy(sealRecordDB = null, isSearching = false, sealNotFound = true, isError = true)
            }
        }
    }

    fun resetState() {
        uiState = uiState.copy(sealRecordDB = null, isSearching = false, sealNotFound = true, isError = false)
        wedCheckSeal = WedCheckSeal()
    }

    fun loadWedCheck(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            // Read CSV data on IO dispatcher
            val csvData = withContext(Dispatchers.IO) {
                readCsvData(context.contentResolver, uri)
            }

            // Insert CSV data into the database
            wedCheckRepo.insertCsvData(csvData)
        }
    }

    private fun readCsvData(contentResolver: ContentResolver, uri: Uri): List<WedCheckRecord> {
        val csvData: MutableList<WedCheckRecord> = mutableListOf()
        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->
                var line: String?
                try {
                    // Read the CSV header (if needed)
                    // Assuming the first line contains the column headers
//                    reader.readLine()

                    // Read each line of the CSV file
                    while (reader.readLine().also { line = it } != null) {
                        // Split the line into fields based on the CSV delimiter (e.g., ',')
                        val fields = line!!.split(",")

                        // Parse fields and create an instance of YourEntity
                        val record = WedCheckRecord(
                            id = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                            speno = fields[0].toInt(),
                            season = fields[1].toInt(),
                            ageClass = fields[2],
                            sex = fields[3],
                            tagIdOne = fields[4],
                            tagIdTwo = fields[5],
                            comments = fields[6],
                            ageYears = fields[7].toInt(),
                            tissueSampled = fields[8],
                            previousPups = fields[9],
                            massPups = fields[10],
                            swimPups = fields[11],
                            photoYears = fields[12],
                        )

                        // Add the parsed entity to the list
                        csvData.add(record)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    // Handle IO exception
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    // Handle number format exception (if field3 cannot be parsed as an Int)
                } finally {
                    // Close the reader
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        // Handle IO exception
                    }
                }
            }
        }
        return csvData
    }
}