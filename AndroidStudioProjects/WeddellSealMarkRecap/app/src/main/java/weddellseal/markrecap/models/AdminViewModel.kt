package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import java.text.SimpleDateFormat
import java.util.Locale

class AdminViewModel(
    application: Application,
    supportingDataRepository: SupportingDataRepository,
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
        val date: String, //TODO, think about the proper date format, should it be UTC?
    )

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    val fileUploads: StateFlow<List<FileUploadEntity>> = supportingDataRepository.fileUploads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Collect as StateFlow

    val successfulUploads: StateFlow<List<FileUploadEntity>> = supportingDataRepository.successfulUploads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Collect as StateFlow
}