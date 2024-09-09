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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.WedCheckRecord
import weddellseal.markrecap.data.WedCheckRepository
import weddellseal.markrecap.data.WedCheckSeal
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

    private val _uiState = MutableStateFlow(
        UiState(
            hasFileAccess = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = false,
        var isSearching: Boolean = false,
        val sealNotFound: Boolean = false,
        val uriForCSVDataLoad: Uri? = null,
        val sealRecordDB: WedCheckRecord? = null,
        val date: String, //TODO, think about the proper date format, should it be UTC?
        val isError: Boolean = false,
        val failedRows: List<String> = emptyList(),
        val totalRows: Int = 0,
        val isWedCheckLoading: Boolean = false,
        val isWedCheckLoaded: Boolean = false,
        val lastWedCheckFileLoaded: String = "",
        val speNoFound: Int = 0,
        val tagIdForSpeNo: String = "",
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
                _uiState.value = uiState.value.copy(hasFileAccess = isGranted)
            }

            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    fun findSealSpeNo(sealTagID: String) {
        _uiState.value = uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            // Switch to the IO dispatcher for database operation
            val returnVal = withContext(Dispatchers.IO) {
                wedCheckRepo.getSealSpeNo(sealTagID)
            }

            if (returnVal != 0) {
                _uiState.value = uiState.value.copy(
                    isSearching = false,
                    sealNotFound = false,
                    speNoFound = returnVal,
                    tagIdForSpeNo = sealTagID
                )
            } else {
                _uiState.value = uiState.value.copy(isSearching = false, sealNotFound = true)
            }
        }
    }

    fun findSealbyTagID(sealTagID: String) {
        _uiState.value = uiState.value.copy(isSearching = true)

        if (sealTagID != "") {
            val searchValue = sealTagID.trim()
            // launch the search for a seal on a separate coroutine
            viewModelScope.launch {

                // Switch to the IO dispatcher for database operation
                val seal: WedCheckRecord = withContext(Dispatchers.IO) {
                    wedCheckRepo.findSealbyTagID(searchValue)
                }
                //ignore the linter, seal can in fact be null
                if (seal != null && seal.speno != 0) {
                    _uiState.value = uiState.value.copy(
                        sealRecordDB = seal,
                        isSearching = false,
                        sealNotFound = false,
                        speNoFound = seal.speno,
                        tagIdForSpeNo = sealTagID
                    )
                    wedCheckSeal = seal.toSeal()
                } else {
                    _uiState.value = uiState.value.copy(
                        sealRecordDB = null,
                        isSearching = false,
                        sealNotFound = true,
                        isError = true
                    )
                }
            }
        }
    }

    fun findSealbySpeNo(speno: Int) {
        _uiState.value = uiState.value.copy(isSearching = true)

        // launch the search for a seal on a separate coroutine
        viewModelScope.launch {

            // Switch to the IO dispatcher for database operation
            val seal: WedCheckRecord = withContext(Dispatchers.IO) {
                wedCheckRepo.findSealbySpeNo(speno)
            }
            //ignore the linter, seal can in fact be null
            if ( seal != null && seal.speno != 0) {
                _uiState.value = uiState.value.copy(
                    sealRecordDB = seal,
                    isSearching = false,
                    sealNotFound = false
                )
                wedCheckSeal = seal.toSeal()
            } else {
                _uiState.value = uiState.value.copy(
                    sealRecordDB = null,
                    isSearching = false,
                    sealNotFound = true,
                    isError = true
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = uiState.value.copy(
            sealRecordDB = null,
            isSearching = false,
            sealNotFound = true,
            isError = false
        )
        wedCheckSeal = WedCheckSeal()
    }

    fun loadWedCheck(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(loading = true, isWedCheckLoading = true)
            try {
                val (csvData, failedRows) = readWedCheckData(context.contentResolver, uri)

                _uiState.value = uiState.value.copy(
                    loading = false,
                    isWedCheckLoaded = true,
                    isWedCheckLoading = false,
                    totalRows = csvData.size + failedRows.size,
                    isError = failedRows.isNotEmpty(),
                    failedRows = failedRows
                )

                // Insert CSV data into the database
                wedCheckRepo.insertCsvData(csvData)

            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    loading = false,
                    isWedCheckLoaded = false,
                    isWedCheckLoading = false,
                    isError = true
                )
            }
        }
    }

    fun updateLastFileNameLoaded(fileName: String) {
        _uiState.value = uiState.value.copy(
            lastWedCheckFileLoaded = fileName
        )
    }

    private fun readWedCheckData(
        contentResolver: ContentResolver,
        uri: Uri
    ): Pair<List<WedCheckRecord>, List<String>> {
        val csvData: MutableList<WedCheckRecord> = mutableListOf()
        val failedRows: MutableList<String> = mutableListOf()

        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream).buffered().use { reader ->
                    // Read the CSV header
                    // Assuming the first line contains the column headers
                    val headerRow = reader.readLine()?.split(",") ?: emptyList()
                    val spenoIndex = headerRow.indexOf("speno")
                    val lastSeenIndex = headerRow.indexOf("last_seen")
                    val ageClassIndex = headerRow.indexOf("ac")
                    val sexIndex = headerRow.indexOf("newsex")
                    val tagOneIndex = headerRow.indexOf("tag1")
                    val tagTwoIndex = headerRow.indexOf("tag2")
                    val noteIndex = headerRow.indexOf("note")
                    val ageIndex = headerRow.indexOf("age")
                    val tissueIndex = headerRow.indexOf("tissue")
                    val pupinMassStudyIndex = headerRow.indexOf("PupinMassStudy")
                    val numPreviousPupsIndex = headerRow.indexOf("NbPreviousPups")
                    val pupinTTStudyIndex = headerRow.indexOf("PupinTTStudy")
                    val momMassMeasurementsIndex = headerRow.indexOf("MomMassMeasurements")
                    val conditionIndex = headerRow.indexOf("cond")
                    val lastPhysioIndex = headerRow.indexOf("last physio")
                    val colonyIndex = headerRow.indexOf("colony")

                    // verify that all columns were found
                    if (listOf(
                            spenoIndex,
                            lastSeenIndex,
                            ageClassIndex,
                            sexIndex,
                            tagOneIndex,
                            tagTwoIndex,
                            noteIndex,
                            ageIndex,
                            tissueIndex,
                            pupinMassStudyIndex,
                            numPreviousPupsIndex,
                            pupinTTStudyIndex,
                            momMassMeasurementsIndex,
                            conditionIndex,
                            lastPhysioIndex,
                            colonyIndex
                        ).all { it != -1 }
                    ) {
                        // Read each line of the CSV file
                        reader.forEachLine { line ->
                            // Split the line into fields based on the CSV delimiter (e.g., ',')
                            val row = line.split(",")
                            try {
                                // Parse fields and create an instance of a WedCheckRecord
                                val record = WedCheckRecord(
                                    id = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                                    speno = row.getOrNull(spenoIndex)?.toIntOrNull() ?: 0,
                                    season = row.getOrNull(lastSeenIndex)?.toIntOrNull() ?: 0,
                                    ageClass = row.getOrNull(ageClassIndex) ?: "",
                                    sex = row.getOrNull(sexIndex) ?: "",
                                    tagIdOne = row.getOrNull(tagOneIndex) ?: "",
                                    tagIdTwo = row.getOrNull(tagTwoIndex) ?: "",
                                    comments = row.getOrNull(noteIndex) ?: "",
                                    ageYears = row.getOrNull(ageIndex)?.toIntOrNull() ?: 0,
                                    tissueSampled = row.getOrNull(tissueIndex) ?: "",
                                    pupinMassStudy = row.getOrNull(pupinMassStudyIndex) ?: "",
                                    numPreviousPups = row.getOrNull(numPreviousPupsIndex) ?: "",
                                    pupinTTStudy = row.getOrNull(pupinTTStudyIndex) ?: "",
                                    momMassMeasurements = row.getOrNull(momMassMeasurementsIndex)
                                        ?: "",
                                    condition = row.getOrNull(conditionIndex) ?: "",
                                    lastPhysio = row.getOrNull(lastPhysioIndex) ?: "",
                                    colony = row.getOrNull(colonyIndex) ?: ""
                                )

                                // Add the parsed entity to the list
                                csvData.add(record)
                            } catch (e: Exception) {
                                // Handle and log any errors in parsing the row
                                e.printStackTrace()
                                // Capture the raw row that failed
                                failedRows.add(line)
                            }
                        }
                    } else {
                        // Handle the case where one or more headers are missing
                        // This could be logging an error, showing a message to the user, etc.
                        throw IllegalArgumentException("CSV file missing required headers")
                    }
                }
            } ?: throw IOException("Unable to open input stream")
        } catch (e: IOException) {
            e.printStackTrace()
            //TODO,  Handle IO exception
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            //TODO
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            //TODO, Handle number format exception (if field3 cannot be parsed as an Int)
        } catch (e: Exception) {
            // Handle any other exceptions
            e.printStackTrace()
            // TODO, Show an error message to the user, log the error, etc.
        }
        return Pair(csvData, failedRows)
    }
}