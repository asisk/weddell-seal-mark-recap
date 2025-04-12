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
import weddellseal.markrecap.frameworks.room.observers.Observers
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.plus

class ObserversViewModel(
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
            fileType = FileType.OBSERVERS.label, // or whatever your enum gives
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            onUploadClick = {},
            onDownloadClick = {},
            lastFilename = null
        )
    )

    val fileState: StateFlow<FileState> = _fileState

    fun updateFileStatus(count: Int)  {
        _fileState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setFileErrorStatus(errorMessage: String) {
        _fileState.update { it.copy(status = FileStatus.ERROR, errorMessage = errorMessage) }
    }

    fun setUploadHandler(handler: () -> Unit) {
        _fileState.update { it.copy(onUploadClick = handler) }
    }

    fun setDownloadHandler(handler: () -> Unit) {
        _fileState.update { it.copy(onDownloadClick = handler) }
    }

    fun setLastFilename(filename: String) {
        _fileState.update { it.copy(lastFilename = filename) }
    }

    fun loadObserversFile(uri: Uri, filename: String) {
        viewModelScope.launch {

            var fileUploadId: Long = -1L

            try {
                // Insert the file and get the fileUploadId
                val fileUploadId = insertFileUpload(filename)

                // Read and process the CSV data
                val (csvData, failedRows) = readAndProcessObserversCsv(uri, fileUploadId)
                if (failedRows.isNotEmpty()) {
                    throw Exception(failedRows[0].errorMessage)
                }

                // Insert the CSV data into the database
                val insertedCount = insertObserversData(fileUploadId, csvData)

                if (insertedCount > 0) {
                    updateFileStatus(insertedCount)
                } else {
                    throw Exception("Failed to insert data")
                }

                // Update the file status based on success or failure
                supportingDataRepository.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.SUCCESS,
                    insertedCount,
                    "successful"
                )

                // Update the UI state based on the result
                updateUiStateObservers(insertedCount, failedRows)

            } catch (e: Exception) {
                val errMessage = e.message ?: "Unknown error"
                setFileErrorStatus(errMessage)
                supportingDataRepository.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    e.message.toString()
                )

                //TODO, remove this once the above is tested out???
                _uiState.value = uiState.value.copy(
                    loading = false,
//                    isObserversLoading = false,
//                    isObserversLoaded = false,
                    isError = true,
//                    totalObserversRows = 0
                )
            }
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

    private suspend fun readAndProcessObserversCsv(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<Observers>, List<FailedRow>> {
        return withContext(Dispatchers.IO) {
            readObserverCsvData(context.contentResolver, uri, fileUploadId)
        }
    }

    private suspend fun insertObserversData(fileUploadId: Long, csvData: List<Observers>): Int {
        return supportingDataRepository.insertObserversData(fileUploadId, csvData)
    }

    private fun updateUiStateObservers(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value =
            uiState.value.copy(
                loading = false,
//                totalObserversRows = insertedCount,
                isError = failedRows.isNotEmpty(),
//                failedObserversRows = failedRows
            )
    }

    private fun readObserverCsvData(
        contentResolver: ContentResolver,
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<Observers>, List<FailedRow>> {
        val csvData: MutableList<Observers> = mutableListOf()
        val failedRows = mutableListOf<FailedRow>()
        var lineNumber = 0

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val initialsIndex = headerRow.indexOf("Initials")

                if (initialsIndex != -1) {
                    reader.forEachLine { line ->
                        lineNumber++ // Increment line number for each iteration

                        try {

                            val row = line.split(",")
                            val record = Observers(
                                observerId = 0,
                                initials = row.getOrNull(initialsIndex) ?: "",
                                fileUploadId = fileUploadId // Foreign key reference
                            )
                            csvData.add(record)

                        } catch (e: Exception) {
                            // Handle and log any errors in parsing the row
                            e.printStackTrace()
                            // Capture the raw row that failed
                            failedRows.add(
                                FailedRow(
                                    rowNumber = lineNumber,
                                    errorMessage = "Invalid number format in row $lineNumber: ${e.localizedMessage}"
                                )
                            )
                        }
                    }
                } else {
                    // one or more headers are missing
                    failedRows.add(
                        FailedRow(
                            rowNumber = 0,
                            errorMessage = "CSV file missing required headers"
                        )
                    )
                    return Pair(csvData, failedRows)
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }

    fun clearObservers() {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            try {
                supportingDataRepository.clearObserversData()
            } catch (e: Exception) {
//                _uiState.value = uiState.value.copy(
//                    isError = true,
//                    errorText = "Error removing observers"
//                )
            }
        }
    }
}