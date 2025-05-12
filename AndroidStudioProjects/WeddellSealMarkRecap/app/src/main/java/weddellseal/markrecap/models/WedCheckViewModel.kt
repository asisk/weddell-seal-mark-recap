package weddellseal.markrecap.models

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
import weddellseal.markrecap.frameworks.room.WedCheckRepository
import weddellseal.markrecap.frameworks.room.files.FailedRow
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import java.io.IOException
import java.io.InputStreamReader

class WedCheckViewModel(
    application: Application,
    private val wedCheckRepo: WedCheckRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val loading: Boolean = false,
        val uriForCSVDataLoad: Uri? = null,
        val isError: Boolean = false,
        val errAcked: Boolean = false,
        val totalRows: Int = 0,
    )

    fun setErrAcked(acked: Boolean) {
        _uiState.update { it.copy(errAcked = acked) }
    }

    // WEDCHECK File State
    private val _wedCheckUploadState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDCHECK.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            message = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            exportFilename = null,
            lastUploadFilename = null,
            recordCount = 0
        )
    )

    val wedCheckUploadState: StateFlow<FileState> = _wedCheckUploadState

    fun resetWedCheckUploadState() {
        _wedCheckUploadState.update {
            it.copy(
                action = FileAction.PENDING,
                status = FileStatus.IDLE,
                message = "",
                exportFilename = null,
                recordCount = 0
            )
        }
    }

    fun updateWedCheckFileStatus(count: Int) {
        _wedCheckUploadState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setWedCheckFileErrorStatus(errorMessage: String) {
        _wedCheckUploadState.update {
            it.copy(
                status = FileStatus.ERROR,
                message = errorMessage,
                recordCount = 0
            )
        }
    }

    fun setWedCheckUploadHandler(handler: () -> Unit) {
        _wedCheckUploadState.update { it.copy(onUploadClick = handler) }
    }

    fun setWedCheckLastFilename(filename: String) {
        _wedCheckUploadState.update { it.copy(lastUploadFilename = filename) }
    }

    fun loadWedCheck(uri: Uri, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert FileUploadEntity and get the fileUploadId
            var fileUploadId = insertFileUploadRecord(filename)

            // 2. Read CSV data
            val (csvData, failedRows) = readAndProcessObserversCsv(uri, fileUploadId)
            if (failedRows.isNotEmpty()) {
                val errMessage = failedRows[0].errorMessage
                setWedCheckFileErrorStatus(errMessage)
                wedCheckRepo.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    errMessage
                )
                return@launch
            }

            // 3. Insert CSV data into the database
            val insertedCount = insertCsvData(fileUploadId, csvData)
            if (insertedCount > 0) {
                updateWedCheckFileStatus(insertedCount)
            } else {
                val errMessage = "No data inserted"
                setWedCheckFileErrorStatus(errMessage)
                wedCheckRepo.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    errMessage
                )
                return@launch
            }

            // 4. Update file upload status based on the result
            wedCheckRepo.updateFileUploadStatus(
                fileUploadId,
                FileStatus.SUCCESS,
                insertedCount,
                "successful"
            )

            // 5. Update the UI state
            updateWedCheckFileStatus(insertedCount)
        }
    }

    private suspend fun insertFileUploadRecord(filename: String): Long {
        return wedCheckRepo.insertFileUpload(
            FileUploadEntity(
                id = 0,
                fileType = FileType.WEDCHECK,
                fileAction = FileAction.UPLOAD.name,
                filename = filename,
                status = FileStatus.IDLE,
                statusMessage = null,
                recordCount = 0
            )
        )
    }

    private fun readAndProcessObserversCsv(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<WedCheckRecord>, List<FailedRow>> {
        return readWedCheckData(context.contentResolver, uri, fileUploadId)
    }

    private fun readWedCheckData(
        contentResolver: ContentResolver,
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<WedCheckRecord>, List<FailedRow>> {
        val csvData: MutableList<WedCheckRecord> = mutableListOf()
        val failedRows: MutableList<FailedRow> = mutableListOf()
        var lineNumber = 0

        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream).buffered().use { reader ->
                    // Read the CSV header
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

                    // Column indices based on the header
                    val requiredHeaders = listOf(
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
                    )

                    // Check if any required headers are missing
                    if (!requiredHeaders.all { it != -1 }) {
                        failedRows.add(
                            FailedRow(
                                rowNumber = 0,
                                errorMessage = "CSV file missing required columns"
                            )
                        )
                        return Pair(csvData, failedRows)
                    }

                    // Read each line of the CSV file
                    reader.forEachLine { line ->
                        lineNumber++ // Increment line number for each iteration
                        // Split the line into fields based on the CSV delimiter (e.g., ',')
                        val row = line.split(",")

                        try {
                            // Parse fields and create an instance of a WedCheckRecord

                            // handle is the possibility that the value is missing or invalid in the row, which is why getOrNull and ?: "" are used
                            val speno = row.getOrNull(spenoIndex)?.toIntOrNull()
                                ?: throw IllegalArgumentException("Invalid or missing speno")

                            val record = WedCheckRecord(
                                speno = speno,
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
                                colony = row.getOrNull(colonyIndex) ?: "",
                                fileUploadId = fileUploadId // This remains the same as it’s coming from your system, not the CSV
                            )

                            // Add the parsed entity to the list
                            csvData.add(record)

                        } catch (e: NumberFormatException) {
                            // Handle and log number format issues
                            failedRows.add(
                                FailedRow(
                                    rowNumber = lineNumber,
                                    errorMessage = "Invalid number format in row $lineNumber: ${e.localizedMessage}"
                                )
                            )
                        } catch (e: Exception) {
                            // Handle and log any errors in parsing the row
                            e.printStackTrace()
                            // Capture the failed row with details like row number and error message
                            failedRows.add(
                                FailedRow(
                                    rowNumber = lineNumber,
                                    errorMessage = "Error in row $lineNumber: ${e.localizedMessage ?: "Unknown error"}"
                                )
                            )
                        }
                    }
                }
            } ?: throw IOException("Unable to open input stream")
        } catch (e: IOException) {
            failedRows.add(
                FailedRow(
                    rowNumber = 0,
                    errorMessage = "I/O error: ${e.localizedMessage}"
                )
            )
        } catch (e: IllegalArgumentException) {
            failedRows.add(
                FailedRow(
                    rowNumber = 0,
                    errorMessage = "Invalid CSV format: ${e.localizedMessage}"
                )
            )
        } catch (e: Exception) {
            failedRows.add(
                FailedRow(
                    rowNumber = 0,
                    errorMessage = "Unknown error: ${e.localizedMessage}"
                )
            )
        }
        return Pair(csvData, failedRows)
    }

    // Helper function to insert WedCheck CSV records into the database
    private suspend fun insertCsvData(
        fileUploadId: Long,
        csvData: List<WedCheckRecord>,
    ): Int {
        return wedCheckRepo.insertCsvData(fileUploadId, csvData).fold(
            onSuccess = { count -> count },
            onFailure = { error ->
                wedCheckRepo.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    error.message.toString()
                )
                throw error
            }
        )
    }
}