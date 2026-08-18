package weddellseal.markrecap.ui.recentobservations

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import weddellseal.markrecap.domain.files.data.FileState
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.ui.UiEvent
import weddellseal.markrecap.ui.admin.ExportType
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import java.io.IOException

class RecentObservationsViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: MutableStateFlow<UiState> = _uiState

    data class UiState(
        val loading: Boolean = false,
        var uriForFileExport: Uri = Uri.EMPTY, // used for record exports
        val errAcked: Boolean = false,
        val archiveAcked: Boolean = false
    )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun setErrAcked(acked: Boolean) {
        _uiState.update { it.copy(errAcked = acked) }
    }

    fun setArchiveAcked(acked: Boolean) {
        _uiState.update { it.copy(archiveAcked = acked) }
    }

    // Used to display the observation records
    val displayObservations: StateFlow<List<DisplayObservation>> =
        observationRepo.currentObservationsDescByID
            .map { observationsToDisplay(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentObservations: StateFlow<List<ObservationRecord>> =
        observationRepo.currentObservationsDescByID
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.Lazily,
                emptyList()
            ) // Collect as StateFlow

    val allObservations: StateFlow<List<ObservationRecord>> =
        observationRepo.allObservationsDescByID
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.Lazily,
                emptyList()
            ) // Collect as StateFlow

    val currentObservationsCount: StateFlow<Int> = currentObservations
        .map { observations: List<ObservationRecord> -> observations.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = 0
        )

    val allObservationsCount: StateFlow<Int> = allObservations
        .map { observations: List<ObservationRecord> -> observations.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = 0
        )

    val exportCurrentObservations: StateFlow<List<ObservationRecord>> =
        observationRepo.currentObservationsByID
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.Lazily,
                emptyList()
            ) // Collect as StateFlow

    // WEDDATACURRENT File State
    private val _wedDataCurrentExportState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDDATACURRENT.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            message = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            exportFilename = "",
            recordCount = 0,
            lastUploadFilename = null
        )
    )

    val wedDataCurrentExportState: StateFlow<FileState> = _wedDataCurrentExportState

    fun resetWedDataCurrentFileState() {
        _wedDataCurrentExportState.update {
            it.copy(
                action = FileAction.PENDING,
                status = FileStatus.IDLE,
                message = "",
                exportFilename = "",
                recordCount = 0
            )
        }
        setErrAcked(false)
        setArchiveAcked(false)
    }

    fun updateWedDataCurrentFileStatus(count: Int, displayNameFromUri: String?) {
        val message = "WedData records exported: $count"
        _wedDataCurrentExportState.update {
            it.copy(
                status = FileStatus.SUCCESS,
                recordCount = count,
                message = message,
                exportFilename = displayNameFromUri
            )
        }
    }

    fun setWedDataCurrentFileErrorStatus(errorMessage: String) {
        _wedDataCurrentExportState.update {
            it.copy(
                status = FileStatus.ERROR,
                message = errorMessage,
                recordCount = 0
            )
        }
    }

    fun setWedDataCurrentExportHandler(handler: () -> Unit) {
        _wedDataCurrentExportState.update { it.copy(onExportClick = handler) }
    }

    // WEDDATAFULL File State
    private val _wedDataFullExportState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDDATAFULL.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            message = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            exportFilename = "",
            lastUploadFilename = null,
            recordCount = 0
        )
    )

    val wedDataFullExportState: StateFlow<FileState> = _wedDataFullExportState

    fun resetWedDataFullFileState() {
        _wedDataFullExportState.update {
            it.copy(
                action = FileAction.PENDING,
                status = FileStatus.IDLE,
                message = "",
                exportFilename = "",
                recordCount = 0
            )
        }
        setErrAcked(false)
    }

    fun updateWedDataFullFileStatus(count: Int, displayNameFromUri: String?) {
        val message = "WedData records exported: $count"
        _wedDataFullExportState.update {
            it.copy(
                status = FileStatus.SUCCESS,
                recordCount = count,
                message = message,
                exportFilename = displayNameFromUri
            )
        }
    }

    fun setWedDataFullFileErrorStatus(errorMessage: String) {
        _wedDataFullExportState.update {
            it.copy(
                status = FileStatus.ERROR,
                message = errorMessage,
                recordCount = 0
            )
        }
    }

    fun setWedDataFullExportHandler(handler: () -> Unit) {
        _wedDataFullExportState.update { it.copy(onExportClick = handler) }
    }

    fun updateURI(uri: Uri) {
        _uiState.update { it.copy(uriForFileExport = uri) }
    }

    fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        return DocumentFile.fromSingleUri(context, uri)?.name
    }

    fun fileNameIsValid(context: Context, uri: Uri): Boolean {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val isCSV = mimeType?.startsWith("text/comma-separated-values")
        val fileName = getDisplayNameFromUri(context, uri)

        if (isCSV == false || fileName == null || !fileName.endsWith(
                ".csv",
                ignoreCase = true
            ) || fileName.contains("invalid")
        ) {
            throw IllegalArgumentException("Invalid file type: $mimeType. Exports should be CSV files.")
        }

        return true
    }

    fun exportRecords(context: Context, exportType: ExportType) {

        viewModelScope.launch {
            val uri = uiState.value.uriForFileExport
            val displayName = getDisplayNameFromUri(context, uri)

            // these stateflow lists are sorted descending from the database, reverse to ascending
            val observations = if (exportType == ExportType.CURRENT) {
                currentObservations.value.sortedBy { it.id }
            } else {
                allObservations.value.sortedBy { it.id }
            }

            if (observations.isEmpty()) {
                setWedDataFullFileErrorStatus("No records available to export.")
                return@launch
            }

            val updateStatus: (Int, String?) -> Unit = when (exportType) {
                ExportType.CURRENT -> ::updateWedDataCurrentFileStatus
                ExportType.ALL -> ::updateWedDataFullFileStatus
            }
            val setErrorStatus: (String) -> Unit = when (exportType) {
                ExportType.CURRENT -> ::setWedDataCurrentFileErrorStatus
                ExportType.ALL -> ::setWedDataFullFileErrorStatus
            }

            try {
                // throw IO exceptions if file is not a writable CSV
                fileNameIsValid(context, uri)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    observationRepo.writeDataToStream(outputStream, observations)
                    updateStatus(observations.size, displayName)
                } ?: run {
                    throw IOException("Failed to write to file: $uri")
                }
            } catch (e: IllegalArgumentException) {
                Log.d("Error", e.message ?: "Unknown error")
                getDisplayNameFromUri(context, uri)
                setErrorStatus(
                    "Failed to write to file. \nInvalid file name:  $displayName"
                )
            } catch (e: Exception) {
                Log.d("Error", e.message ?: "Failed to write to file")
                setErrorStatus(e.message ?: "Failed to write to file")
            }
        }
    }

    fun markObservationsAsDeleted() {
        viewModelScope.launch {
            observationRepo.softDeleteAllObservations()
        }
    }

    fun onArchiveAttempt() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowArchiveDialog)
        }
    }

    fun onDeleteAllAttempt() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowDeleteRecordsDialog)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            observationRepo.deleteAll()
        }
    }
}