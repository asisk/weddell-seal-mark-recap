package weddellseal.markrecap.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.files.FilesRepository

class AdminViewModel(
    application: Application,
    supportingDataRepository: FilesRepository,
) : AndroidViewModel(application) {

    data class AdminUiState(
        val navRailSelection: Int = 1 // default to dashboard
    )

    private val _adminUiState = MutableStateFlow(
        AdminUiState()
    )

    val adminUiState: StateFlow<AdminUiState> = _adminUiState

    val successfulUploads: StateFlow<List<FileUploadEntity>> =
        supportingDataRepository.successfulUploads
            .stateIn(viewModelScope, SharingStarted.Companion.Lazily, emptyList())

    fun navToArchiveView(selection : Int) {
        _adminUiState.value = AdminUiState(navRailSelection = selection)
    }
}