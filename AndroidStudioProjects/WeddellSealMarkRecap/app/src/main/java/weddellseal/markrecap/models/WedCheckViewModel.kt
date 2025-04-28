package weddellseal.markrecap.models

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.frameworks.room.WedCheckRepository
import weddellseal.markrecap.frameworks.room.WedCheckSeal
import weddellseal.markrecap.frameworks.room.files.FailedRow
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.ui.file.FileType
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
        UiState(date = dateNowFormatted())
    )
    val uiState: StateFlow<UiState> = _uiState

    data class UiState(
        val loading: Boolean = false,
        var isSearching: Boolean = false,
        val sealNotFound: Boolean = false,
        val uriForCSVDataLoad: Uri? = null,
        val sealRecordDB: WedCheckRecord? = null,
        val date: String, //TODO, think about the proper date format, should it be UTC?
        val isError: Boolean = false,
        val errAcked: Boolean = false,
        val totalRows: Int = 0,
        val speNoFound: Int = 0,
        val tagIdForSpeNo: String = "",
    )

    fun setErrAcked(acked: Boolean) {
        _uiState.update { it.copy(errAcked = acked) }
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

    fun dateNowFormatted(): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss aaa z", Locale.US)
            .format(System.currentTimeMillis())

    // WEDCHECK File State
    private val _wedCheckUploadState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDCHECK.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            errorMessage = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            downloadFilename = null,
            lastFilename = null
        )
    )

    val wedCheckUploadState: StateFlow<FileState> = _wedCheckUploadState

    fun updateWedCheckFileStatus(count: Int) {
        _wedCheckUploadState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setWedCheckFileErrorStatus(errorMessage: String) {
        _wedCheckUploadState.update {
            it.copy(
                status = FileStatus.ERROR,
                errorMessage = errorMessage
            )
        }
    }

    fun setWedCheckUploadHandler(handler: () -> Unit) {
        _wedCheckUploadState.update { it.copy(onUploadClick = handler) }
    }

    fun setWedCheckLastFilename(filename: String) {
        _wedCheckUploadState.update { it.copy(lastFilename = filename) }
    }

    // WEDDATACURRENT File State
    private val _wedDataCurrentExportState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDDATACURRENT.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            errorMessage = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            downloadFilename = null,
            lastFilename = null
        )
    )

    val wedDataCurrentExportState: StateFlow<FileState> = _wedDataCurrentExportState

    fun updateWedDataCurrentFileStatus(count: Int) {
        _wedDataCurrentExportState.update {
            it.copy(
                status = FileStatus.SUCCESS,
                recordCount = count
            )
        }
    }

    fun setWedDataCurrentFileErrorStatus(errorMessage: String) {
        _wedDataCurrentExportState.update {
            it.copy(
                status = FileStatus.ERROR,
                errorMessage = errorMessage
            )
        }
    }

    fun setWedDataCurrentExportHandler(handler: () -> Unit) {
        _wedDataCurrentExportState.update { it.copy(onExportClick = handler) }
    }

    fun setWedDataCurrentLastFilename(filename: String) {
        _wedDataCurrentExportState.update { it.copy(lastFilename = filename) }
    }

    // WEDDATAFULL File State
    private val _wedDataFullExportState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDDATAFULL.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            errorMessage = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            downloadFilename = null,
            lastFilename = null
        )
    )

    val wedDataFullExportState: StateFlow<FileState> = _wedDataFullExportState

    fun updateWedDataFullFileStatus(count: Int) {
        _wedDataFullExportState.update { it.copy(status = FileStatus.SUCCESS, recordCount = count) }
    }

    fun setWedDataFullFileErrorStatus(errorMessage: String) {
        _wedDataFullExportState.update {
            it.copy(
                status = FileStatus.ERROR,
                errorMessage = errorMessage
            )
        }
    }

    fun setWedDataFullExportHandler(handler: () -> Unit) {
        _wedDataFullExportState.update { it.copy(onExportClick = handler) }
    }

    fun setWedDataFullLastFilename(filename: String) {
        _wedDataFullExportState.update { it.copy(lastFilename = filename) }
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
        // clear the last seal from the wedcheck viewmodel
        _uiState.value = uiState.value.copy(
            sealRecordDB = null,
            isSearching = true,
            sealNotFound = true,
            isError = false
        )
        Log.d("findSealbyTagID in WedCheckViewModel", "Clearing wedcheck seal")
        wedCheckSeal = WedCheckSeal()

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
                    delay(1500) // Wait for 1500 milliseconds
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
            if (seal != null && seal.speno != 0) {
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

    fun loadWedCheck(uri: Uri, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert FileUploadEntity and get the fileUploadId
            var fileUploadId = insertFileUploadRecord(filename)

            // 2. Read CSV data
            val (csvData, failedRows) = readWedCheckData(context.contentResolver, uri, fileUploadId)
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

//    private fun readWedCheckData(
//        uri: Uri,
//        fileUploadId: Long
//    ): Pair<List<WedCheckRecord>, List<FailedRow>> {
//        return readWedCheckData(context.contentResolver, uri, fileUploadId)
//    }

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