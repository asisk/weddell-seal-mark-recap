package weddellseal.markrecap

/*
 * Primary model that acts as a communication center between the
 * ObservationSaverRepository (data) and the UI.
 * The UI no longer needs to worry about the origin of the data.
 * ViewModel instances survive Activity/Fragment recreation.
 */

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File


class HomeViewModel(
    application: Application,
    val observationSaver: ObservationSaverRepository,
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()
    // allows the home screen to observe LiveData as observations are saved
    val observationsFlow: Flow<List<ObservationLogEntry>> = observationSaver.observations.asFlow()

    data class UiState(
        val loading: Boolean = true,
        var uriForCSVWrite: Uri? = null,
        var observations: List<ObservationLogEntry> = emptyList()
    )

    var uiState by mutableStateOf(UiState(
        observations = observationSaver._observations
        )
    )
        private set

    fun formatDateTime(timeInMillis: Long): String {
        return DateUtils.formatDateTime(context, timeInMillis, DateUtils.FORMAT_ABBREV_ALL)
    }
    fun updateURI(uri: Uri) {
        uiState = uiState.copy(
            uriForCSVWrite = uri
        )
        // may need to throw an error if no uri returned from file picker call on the home screen
    }

//    fun exportLogs() {
//        viewModelScope.launch {
//            val file = uriToFile(context, uiState.uriForCSVWrite!!)
//
//            if (file != null) {
//                observationSaver.saveObservations(file)
//            } else {
//                throw NoUriSelectedException("No URI was selected")
//            }
//
//            uiState = uiState.copy(
//                loading = false,
//            )
//        }
//
//    }

    fun exportLogs() {
        viewModelScope.launch {
            try {
                // Ensure there's a URI selected for writing the CSV file
                val uriForCSVWrite = uiState.uriForCSVWrite
                if (uriForCSVWrite == null) {
                    throw NoUriSelectedException("No URI was selected")
                }

                // Convert the URI to a file
                val file = uriToFile(context, uriForCSVWrite)

                // Check if the file is not null
                if (file != null) {
                    // Create an Intent to share the file
                    // Save observations to the file
                    observationSaver.writeDataToFile(uriForCSVWrite)

                } else {
                    throw NoUriSelectedException("No file found for the selected URI")
                }

                // Update the UI state (e.g., loading state)
                uiState = uiState.copy(
                    loading = false,
                )
            } catch (e: Exception) {
                // Handle any exceptions, e.g., log an error message or show a user-friendly error
                e.printStackTrace()
            }
        }
    }


    fun uriToFile(context: Context, uri: Uri): File? {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        if (documentFile != null) {
            val displayName = documentFile.name
            val externalDir = Environment.getExternalStorageDirectory() // or another appropriate directory
            return File(externalDir, displayName)
        }
        return null
    }

    fun delete(observationLog: ObservationLog) {
       // viewModelScope.launch {
       //     db.logDao().delete(observationLog.toLogEntry())
       //     loadLogs()
       // }
    }
}

class NoUriSelectedException(message: String = "No URI was selected") : IllegalArgumentException(message)

class HomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return HomeViewModel(app, app.observationSaver) as T
    }
}