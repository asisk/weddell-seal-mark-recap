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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.files.FailedRow
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.WedCheckRepository
import weddellseal.markrecap.frameworks.room.WedCheckSeal
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.ui.file.FileStatus
import weddellseal.markrecap.ui.file.FileType
import weddellseal.markrecap.frameworks.room.wedCheck.toSeal
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
        val totalRows: Int = 0,
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


    private val _fileStates = MutableStateFlow(
        FileType.values().associateWith { fileType ->
            FileState(
                fileType = fileType.label,
                action = FileAction.PENDING,
                status = FileStatus.IDLE,
                onUploadClick = {}, // will override this when the Admin Screen view loads
                onDownloadClick = {}, // will override this when the Admin Screen view loads
                downloadFilename = null,
                lastFilename = null
            )
        }
    )
    val fileStates: StateFlow<Map<FileType, FileState>> = _fileStates
    fun updateFileStatus(type: FileType, status: FileStatus, recordCount: Int) {
        _fileStates.update { currentMap ->
            currentMap + (type to currentMap.getValue(type)
                .copy(status = status, recordCount = recordCount))
        }
    }

    fun setFileErrorStatus(type: FileType, status: FileStatus, errorMessage: String) {
        _fileStates.update { currentMap ->
            currentMap + (type to currentMap.getValue(type)
                .copy(status = status, errorMessage = errorMessage))
        }
    }

    fun setFileHandler(type: FileType, action: FileAction, handler: () -> Unit) {
        if (action == FileAction.UPLOAD) {
            _fileStates.update { currentMap ->
                currentMap + (type to currentMap.getValue(type).copy(onUploadClick = handler))
            }
        } else if (action == FileAction.DOWNLOAD) {
            _fileStates.update { currentMap ->
                currentMap + (type to currentMap.getValue(type).copy(onDownloadClick = handler))
            }
        }
    }

    fun setLastFilename(type: FileType, filename: String) {
        _fileStates.update { map ->
            map + (type to map.getValue(type).copy(lastFilename = filename))
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

    fun resetState() {
        _uiState.value = uiState.value.copy(
            sealRecordDB = null,
            isSearching = false,
            sealNotFound = true,
            isError = false
        )
        wedCheckSeal = WedCheckSeal()
    }


    fun loadWedCheck(uri: Uri, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var fileUploadId: Long = -1L
            try {

                // 1. Insert FileUploadEntity and get the fileUploadId
                fileUploadId = insertFileUploadRecord(filename)

                // 2. Read CSV data
                val (csvData, failedRows) = readWedCheckData(uri, fileUploadId)
                if (failedRows.isNotEmpty()) {
                    throw Exception(failedRows[0].errorMessage)
                }

                // 3. Insert CSV data into the database
                val insertedCount = insertCsvData(fileUploadId, csvData)

                if (insertedCount > 0) {
                    updateFileStatus(FileType.WEDCHECK, FileStatus.SUCCESS, insertedCount)
                } else {
                    throw Exception("Failed to insert data")
                }

                // 4. Update file upload status based on the result
                wedCheckRepo.updateFileUploadStatus(fileUploadId, FileStatus.SUCCESS, insertedCount, "successful")


                // 5. Update the UI state
                updateUIState(insertedCount, failedRows)
                if (failedRows.isNotEmpty()) {
                    throw Exception(failedRows[0].errorMessage)
                }

            } catch (e: Exception) {
                val errMessage = e.message ?: "Unknown error"
                setFileErrorStatus(FileType.WEDCHECK, FileStatus.ERROR, errMessage)
                wedCheckRepo.updateFileUploadStatus(
                    fileUploadId,
                    FileStatus.ERROR,
                    0,
                    e.message.toString()
                )
            }
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

    private fun readWedCheckData(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<WedCheckRecord>, List<FailedRow>> {
        return readWedCheckData(context.contentResolver, uri, fileUploadId)
    }

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

    private fun updateUIState(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value = uiState.value.copy(
            loading = false,
            totalRows = insertedCount,
            isError = failedRows.isNotEmpty(),
        )
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
}