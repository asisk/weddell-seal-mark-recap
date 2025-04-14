package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.files.FailedRow
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.ui.file.FileType
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class SealColoniesViewModel(
    application: Application,
    private val supportingDataRepository: SupportingDataRepository,
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

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
        val uriForCSVDataLoad: Uri? = null,
        val date: String, //TODO, think about the proper date format, should it be UTC?
        val isError: Boolean = false,
        val totalRows: Int = 0,
    )

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val _fileState = MutableStateFlow(
        FileState(
            fileType = FileType.COLONIES.label, // or whatever your enum gives
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            onUploadClick = {},
            onExportClick = {},
            lastFilename = null
        )
    )
    val fileState: StateFlow<FileState> = _fileState

    fun updateFileStatus(count: Int) {
        _fileState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setFileErrorStatus(errorMessage: String) {
        _fileState.update { it.copy(status = FileStatus.ERROR, errorMessage = errorMessage) }
    }

    fun setUploadHandler(handler: () -> Unit) {
        _fileState.update { it.copy(onUploadClick = handler) }
    }

    fun setDownloadHandler(handler: () -> Unit) {
        _fileState.update { it.copy(onExportClick = handler) }
    }

    fun setLastFilename(filename: String) {
        _fileState.update { it.copy(lastFilename = filename) }
    }

    fun loadSealColoniesFile(uri: Uri, filename: String) {
        viewModelScope.launch {

//            var fileUploadId: Long = -1L

//            try {
                // Insert the file and get the fileUploadId
                val fileUploadId = insertFileUpload(filename)

                // Read and process the CSV data
                val (csvData, failedRows) = readAndProcessColonyCsv(uri, fileUploadId)
                if (failedRows.isNotEmpty()) {
                    val errMessage = failedRows[0].errorMessage
                    setFileErrorStatus(errMessage)
                    supportingDataRepository.updateFileUploadStatus(
                        fileUploadId,
                        FileStatus.ERROR,
                        0,
                        errMessage
                    )
                    return@launch
                }

                // Insert the CSV data into the database
                val insertedCount = insertColonyData(fileUploadId, csvData)
                if (insertedCount > 0) {
                    updateFileStatus( insertedCount)
                } else {
                    val errMessage = "No data inserted"
                    setFileErrorStatus(errMessage)
                    supportingDataRepository.updateFileUploadStatus(
                        fileUploadId,
                        FileStatus.ERROR,
                        0,
                        errMessage
                    )
                    return@launch
                }


                // Update the file status based on success or failure
                supportingDataRepository.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.SUCCESS,
                    insertedCount,
                    "successful"
                )

                // Update the UI state based on the result
                updateFileStatus( insertedCount)

//            } catch (e: Exception) {
//                val errMessage = e.message ?: "Unknown error"
//                setFileErrorStatus(errMessage)
//                supportingDataRepository.updateFileUploadStatus(
//                    fileUploadId,
//                    FileStatus.ERROR,
//                    0,
//                    e.message.toString()
//                )
//
//                //TODO, remove this once the above is tested out???
//                _uiState.value = uiState.value.copy(
//                    loading = false,
////                    isColonyLocationsLoading = false,
////                    isColonyLocationsLoaded = false,
//                    isError = true,
////                    totalColoniesRows = 0
//                )
//            }
        }
    }

    private suspend fun insertFileUpload(filename: String): Long {
        return supportingDataRepository.insertFileUpload(
            FileUploadEntity(
                id = 0,
                fileType = FileType.OBSERVERS,
                fileAction = FileAction.UPLOAD.name,
                filename = filename,
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
    }

    private suspend fun readAndProcessColonyCsv(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<SealColony>, List<FailedRow>> {
        return withContext(Dispatchers.IO) {
            readSealColoniesCsvData(context.contentResolver, uri, fileUploadId)
        }
    }

    private suspend fun insertColonyData(fileUploadId: Long, csvData: List<SealColony>): Int {
        return supportingDataRepository.insertColoniesData(fileUploadId, csvData)
    }

    private fun updateUiStateColonies(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value =
            uiState.value.copy(
                loading = false,
//                totalColoniesRows = insertedCount,
                isError = failedRows.isNotEmpty(),
//                failedColoniesRows = failedRows
            )
    }

    private fun readSealColoniesCsvData(
        contentResolver: ContentResolver,
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<SealColony>, List<FailedRow>> {
        val csvData: MutableList<SealColony> = mutableListOf()
        val failedRows = mutableListOf<FailedRow>()
        var lineNumber = 0

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()

                // Define the required headers and their corresponding column names
                val inOutIndex = headerRow.indexOf("In/Out")
                val locationIndex = headerRow.indexOf("Location")
                val nLimitIndex = headerRow.indexOf("N_Limit")
                val sLimitIndex = headerRow.indexOf("S_Limit")
                val wLimitIndex = headerRow.indexOf("W_Limit")
                val eLimitIndex = headerRow.indexOf("E_Limit")
                val adjLatIndex = headerRow.indexOf("Adj_Lat")
                val adjLongIndex = headerRow.indexOf("Adj_Long")

                val requiredHeaders = listOf(
                    inOutIndex,
                    locationIndex,
                    nLimitIndex,
                    sLimitIndex,
                    wLimitIndex,
                    eLimitIndex,
                    adjLatIndex,
                    adjLongIndex
                )

                // Check if any required headers are missing
                if (!requiredHeaders.all { it != -1 }) {
                    // Handle missing headers (e.g., throw an error, log, or show a message to the user)
                    failedRows.add(
                        FailedRow(
                            rowNumber = 0,
                            errorMessage = "CSV file missing required headers"
                        )
                    )
                    return Pair(csvData, failedRows)
                }

                reader.forEachLine { line ->
                    lineNumber++ // Increment line number for each iteration
                    try {
                        val row = line.split(",")

                        val record = SealColony(
                            colonyId = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                            inOut = row.getOrNull(inOutIndex) ?: "",
                            location = row.getOrNull(locationIndex) ?: "",
                            nLimit = row.getOrNull(nLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            sLimit = row.getOrNull(sLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            wLimit = row.getOrNull(wLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            eLimit = row.getOrNull(eLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            adjLat = row.getOrNull(adjLatIndex)?.toDoubleOrNull() ?: 0.0,
                            adjLong = row.getOrNull(adjLongIndex)?.toDoubleOrNull() ?: 0.0,
                            fileUploadId = fileUploadId // Foreign key reference
                        )

                        // Add the parsed entity to the list
                        csvData.add(record)

                    } catch (e: Exception) {
                        // Handle and log any errors in parsing the row
                        e.printStackTrace()
                        // Capture the raw row that failed
                        FailedRow(
                            rowNumber = lineNumber,
                            errorMessage = "Error in row $lineNumber: ${e.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }

    fun clearColonies() {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            try {
                supportingDataRepository.clearColonyData()
            } catch (e: Exception) {
//                _uiState.value = uiState.value.copy(
//                    isError = true,
//                    errorText = "Error removing colonies"
//                )
            }
        }
    }
}