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
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import weddellseal.markrecap.frameworks.room.observations.ObservationLogEntry
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import java.io.File

class RecentObservationsViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    // allows for the screen to observe LiveData as observations are saved
    val observationsFlow: Flow<List<ObservationLogEntry>> = observationRepo.observations.asFlow()

    data class UiState(
        val loading: Boolean = true,
        var uriForCSVWrite: Uri? = null,
        var observations: List<ObservationLogEntry> = emptyList(),
        var isError: Boolean = false,
        var errorMessage: String = ""
    )

    var uiState by mutableStateOf(
        UiState()
    )
        private set

    fun updateURI(uri: Uri) {
        uiState = uiState.copy(
            uriForCSVWrite = uri
        )
        // may need to throw an error if no uri returned from file picker call
    }

    fun exportLogs(context: Context) {
        viewModelScope.launch {
            try {
                // Ensure there's a URI selected for writing the CSV file
                val uriForCSVWrite =
                    uiState.uriForCSVWrite ?: throw NoUriSelectedException("No URI was selected")

                // Convert the URI to a file
                val file = uriToFile(context, uriForCSVWrite)

                // Check if the file is not null
                if (file != null) {
                    // Create an Intent to share the file
                    // Save observations to the file
                    val currentObservations = observationRepo.getCurrentObservations()
                    if (currentObservations.isNotEmpty()) {
                        observationRepo.writeDataToFile(
                            uriForCSVWrite,
                            context.contentResolver,
                            currentObservations
                        )
                    } else {
                        uiState = uiState.copy(
                            isError = true,
                            loading = false,
                            errorMessage = "No Records to Export"
                        )
                    }

                } else {
                    uiState = uiState.copy(
                        isError = true,
                        loading = false,
                        errorMessage = "No File Found"
                    )
                }

                // Update the UI state (e.g., loading state)
                uiState = uiState.copy(isError = false, loading = false)

            } catch (e: Exception) {
                Log.d("Error", e.message ?: "Unknown error")
                uiState =
                    uiState.copy(isError = true, loading = false, errorMessage = "Unknown Error")
            }
        }
    }

    fun exportAllLogs(context: Context) {
        viewModelScope.launch {
            try {
                // Ensure there's a URI selected for writing the CSV file
                val uriForCSVWrite =
                    uiState.uriForCSVWrite ?: throw NoUriSelectedException("No URI was selected")

                // Convert the URI to a file
                val file = uriToFile(context, uriForCSVWrite)

                // Check if the file is not null
                if (file != null) {
                    // Create an Intent to share the file
                    // Save observations to the file
                    val allObservations = observationRepo.getAllObservations()
                    if (allObservations.isNotEmpty()) {
                        observationRepo.writeDataToFile(
                            uriForCSVWrite,
                            context.contentResolver,
                            allObservations
                        )
                    } else {
                        uiState = uiState.copy(
                            isError = true,
                            loading = false,
                            errorMessage = "No Records to Export"
                        )
                    }


                } else {
                    uiState = uiState.copy(
                        isError = true,
                        loading = false,
                        errorMessage = "File not found"
                    )
                }

                // Update the UI state (e.g., loading state)
                uiState = uiState.copy(isError = false, loading = false)

            } catch (e: Exception) {
                uiState =
                    uiState.copy(isError = true, loading = false, errorMessage = "Unknown Error")
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        if (documentFile != null) {
            val displayName = documentFile.name ?: return null // Return null if name is null
            val externalDir = Environment.getExternalStorageDirectory()
            return File(externalDir, displayName)
        }
        return null
    }

    fun markObservationsAsDeleted() {
        viewModelScope.launch {

            observationRepo.softDeleteAllObservations()
        }
    }
}

class NoUriSelectedException(message: String = "No URI was selected") :
    IllegalArgumentException(message)