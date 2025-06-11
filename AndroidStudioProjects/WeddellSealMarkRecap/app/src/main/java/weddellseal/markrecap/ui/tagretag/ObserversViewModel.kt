package weddellseal.markrecap.ui.tagretag

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.files.data.FailedRow
import weddellseal.markrecap.domain.files.data.FileState
import weddellseal.markrecap.frameworks.room.files.FilesRepository
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.Observers
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import java.io.IOException
import java.io.InputStreamReader

class ObserversViewModel(
    application: Application,
    private val observersRepository: ObserversRepository,
    private val filesRepository: FilesRepository,
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    data class UiState(
        val loading: Boolean = false,
        val uriForCSVDataLoad: Uri? = null,
        val errAcked: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun setErrAcked(acked: Boolean) {
        _uiState.update { it.copy(errAcked = acked) }
    }

    private val _fileState = MutableStateFlow(
        FileState(
            fileType = FileType.OBSERVERS.label, // or whatever your enum gives
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            message = "",
            onUploadClick = {},
            onExportClick = {},
            lastUploadFilename = null,
            recordCount = 0
        )
    )

    val fileState: StateFlow<FileState> = _fileState

    fun resetFileState() {
        _fileState.update {
            it.copy(
                action = FileAction.PENDING,
                status = FileStatus.IDLE,
                message = "",
                lastUploadFilename = null,
                recordCount = 0
            )
        }
    }


    fun updateFileStatus(count: Int) {
        _fileState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setFileErrorStatus(errorMessage: String) {
        _fileState.update { it.copy(status = FileStatus.ERROR, message = errorMessage, recordCount = 0) }
    }

    fun setUploadHandler(handler: () -> Unit) {
        _fileState.update { it.copy(onUploadClick = handler) }
    }

    fun setLastFilename(filename: String) {
        _fileState.update { it.copy(lastUploadFilename = filename) }
    }

    fun loadObserversFile(uri: Uri, filename: String) {
        viewModelScope.launch {
            // Insert the file and get the fileUploadId
            val fileUploadId = insertFileUpload(filename)

            // Read and process the CSV data
            val (csvData, failedRows) = readAndProcessObserversCsv(uri, fileUploadId)
            if (failedRows.isNotEmpty()) {
                val errMessage = failedRows[0].errorMessage
                setFileErrorStatus(errMessage)
                filesRepository.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    errMessage
                )
                return@launch
            }

            // Insert the CSV data into the database
            val insertedCount = insertObserversData(fileUploadId, csvData)
            if (insertedCount > 0) {
                updateFileStatus(insertedCount)
            } else {
                val errMessage = "No data inserted"
                setFileErrorStatus(errMessage)
                filesRepository.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    errMessage
                )
                return@launch
            }

            // Update the file status based on success or failure
            filesRepository.updateFileUploadStatus(
                fileUploadId,
                FileStatus.SUCCESS,
                insertedCount,
                "successful"
            )

            // Update the UI state based on the result
            updateFileStatus(insertedCount)
        }
    }

    private suspend fun insertFileUpload(filename: String): Long {
        return filesRepository.insertFileUpload(
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
        return observersRepository.insertObserversData(fileUploadId, csvData)
    }

    private fun updateUiStateObservers(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value =
            uiState.value.copy(
                loading = false,
//                totalObserversRows = insertedCount,
//                isError = failedRows.isNotEmpty(),
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
                observersRepository.clearObserversData()
            } catch (e: Exception) {
//                _uiState.value = uiState.value.copy(
//                    isError = true,
//                    errorText = "Error removing observers"
//                )
            }
        }
    }
}