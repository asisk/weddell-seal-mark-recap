package weddellseal.markrecap.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity

class AdminViewModel(
    application: Application,
    supportingDataRepository: SupportingDataRepository,
) : AndroidViewModel(application) {

    data class AdminUiState(
        val isError: Boolean = false,
        val isErrorAckd: Boolean = false
    )

    private val _adminUiState = MutableStateFlow(
        AdminUiState(
            isError = false,
            isErrorAckd = false
        )
    )
    val adminUiState: StateFlow<AdminUiState> = _adminUiState

    val successfulUploads: StateFlow<List<FileUploadEntity>> =
        supportingDataRepository.successfulUploads
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Collect as StateFlow

    fun setErr(error: Boolean) {
        _adminUiState.value = adminUiState.value.copy(
            isError = error
        )
    }

    fun setErrAck(errorAcknowledged: Boolean) {
        _adminUiState.value = adminUiState.value.copy(
            isErrorAckd = errorAcknowledged
        )
    }
}