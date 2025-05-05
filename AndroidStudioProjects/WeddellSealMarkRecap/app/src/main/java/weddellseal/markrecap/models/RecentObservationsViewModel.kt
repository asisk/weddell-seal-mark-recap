package weddellseal.markrecap.models

/*
 * Primary model that acts as a communication center between the
 * ObservationRepository (data) and the UI.
 * The UI no longer needs to worry about the origin of the data.
 * ViewModel instances survive Activity/Fragment recreation.
 */

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType

class RecentObservationsViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
) : AndroidViewModel(application) {

    data class UiState(
        val loading: Boolean,
        var uriForCSVWrite: Uri = Uri.EMPTY, // used for record exports
        var observations: List<ObservationLogEntry> = emptyList(), // used in recent observations screen
    )

    var uiState by mutableStateOf(
        UiState(
            loading = false,
        )
    )
        private set

    // allows for the screen to observe LiveData as observations are saved
    val observationsFlow: Flow<List<ObservationLogEntry>> =
        observationRepo.currentObservations.asFlow()

    val allObservationsFlow: Flow<List<ObservationLogEntry>> =
        observationRepo.allObservations.asFlow()

    val currentObservationsCount: StateFlow<Int> = observationsFlow
        .map { observations: List<ObservationLogEntry> -> observations.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val allObservationsCount: StateFlow<Int> = allObservationsFlow
        .map { observations: List<ObservationLogEntry> -> observations.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    // WEDDATACURRENT File State
    private val _wedDataCurrentExportState = MutableStateFlow(
        FileState(
            fileType = FileType.WEDDATACURRENT.label,
            action = FileAction.PENDING,
            status = FileStatus.IDLE,
            message = "",
            onUploadClick = {}, // will override this when the Admin Screen view loads
            onExportClick = {}, // will override this when the Admin Screen view loads
            exportFilename = null,
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
                lastUploadFilename = null,
                recordCount = 0
            )
        }
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
            exportFilename = null,
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
                lastUploadFilename = null,
                recordCount = 0
            )
        }
    }

    fun updateWedDataFullFileStatus(count: Int, displayNameFromUri: String?) {
        val message = "WedData records exported: $count"
        _wedDataFullExportState.update {
            it.copy(
                status = FileStatus.SUCCESS,
                recordCount = count,
                message = message,
                lastUploadFilename = displayNameFromUri
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
        uiState = uiState.copy(
            uriForCSVWrite = uri
        )
    }

    fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        return DocumentFile.fromSingleUri(context, uri)?.name
    }

    fun exportCurrentRecords(context: Context) {
        viewModelScope.launch {
            try {
                val currentObservations = observationRepo.getCurrentObservations()
                if (currentObservations.isEmpty()) {
                    setWedDataCurrentFileErrorStatus("No records available to export.")
                    return@launch
                }
                val uri = uiState.uriForCSVWrite
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    observationRepo.writeDataToStream(outputStream, currentObservations)
                    updateWedDataCurrentFileStatus(
                        currentObservations.size,
                        getDisplayNameFromUri(context, uri)
                    )
                } ?: run {
                    setWedDataCurrentFileErrorStatus("Unable to open file for writing.")
                }
            } catch (e: Exception) {
                Log.d("Error", e.message ?: "Unknown error")
                setWedDataCurrentFileErrorStatus(e.message.toString())
            }
        }
    }

    fun exportAllRecords(context: Context) {
        viewModelScope.launch {
            try {
                val allObservations = observationRepo.getAllObservations()
                if (allObservations.isEmpty()) {
                    setWedDataFullFileErrorStatus("No records available to export.")
                    return@launch
                }

                val uri = uiState.uriForCSVWrite
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    observationRepo.writeDataToStream(outputStream, allObservations)
                    updateWedDataFullFileStatus(
                        allObservations.size,
                        getDisplayNameFromUri(context, uri)
                    )
                } ?: run {
                    setWedDataFullFileErrorStatus("Unable to open file for writing.")
                }
            } catch (e: Exception) {
                Log.d("Error", e.message ?: "Unknown error")
                setWedDataFullFileErrorStatus(e.message.toString())
            }
        }
    }

    fun markObservationsAsDeleted() {
        viewModelScope.launch {
            observationRepo.softDeleteAllObservations()
        }
    }
}