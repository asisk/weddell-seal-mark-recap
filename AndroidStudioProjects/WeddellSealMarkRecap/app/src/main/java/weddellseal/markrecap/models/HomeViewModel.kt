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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import weddellseal.markrecap.ObservationLogApplication
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.ui.screens.LocationInfo
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Home Screen model
 */
class HomeViewModel(
    application: Application,
    private val observationRepo: ObservationRepository
) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = true,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        var colonyLocations: List<String> = emptyList(),
        val date: String,
    )

    var uiState by mutableStateOf(
        UiState(
            hasFileAccess = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )
        private set

    data class StudyArea(
        val location: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
    )

    var studyAreaState by mutableStateOf(StudyArea(location = "Default A"))
        private set

    fun isValid(): Boolean {
//        if (!observationSaver.isEmpty() && !uiState.isSaving) {
        if (observationRepo.canWriteStudyAreas() && !uiState.isSaving) {
            return true
        }
        return false
//        return !observationSaver.isEmpty() && !uiState.isSaving
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                uiState = uiState.copy(hasFileAccess = isGranted)
            }
            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    private fun readCsvData(contentResolver: ContentResolver, uri: Uri): List<LocationInfo> {
        val locationInfoList = mutableListOf<LocationInfo>()
        val dropdownList = mutableListOf<String>()
        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->
                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val locationIndex = headerRow.indexOf("Location")
                val latitudeIndex = headerRow.indexOf("Adj_Lat")
                val longitudeIndex = headerRow.indexOf("Adj_Long")

                reader.forEachLine { line ->
                    val row = line.split(",")
                    if (row.size >= 3 && locationIndex != -1 && latitudeIndex != -1 && longitudeIndex != -1) {
                        val location = row.getOrNull(locationIndex)
                        dropdownList.add(location.toString())
                        val latitude = row.getOrNull(latitudeIndex)?.toDoubleOrNull() ?: 0.0
                        val longitude = row.getOrNull(longitudeIndex)?.toDoubleOrNull() ?: 0.0
                        if (location != null) {
                            val locationInfo = LocationInfo(location, latitude, longitude)
                            locationInfoList.add(locationInfo)
                        }
                    } else {
                        // Handle invalid row or missing columns
                    }
                }
                uiState = uiState.copy(colonyLocations = dropdownList)
            }
        }

        return locationInfoList
    }

    private fun extractDropdownValues(locationInfoList: List<LocationInfo>): List<String> {
        return locationInfoList.map { it.location }
    }

    fun loadStudyAreaFile(uri: Uri) {
        val csvData = readCsvData(context.contentResolver, uri)
        // Update dropdown with dropdownValues
        val studyAreas = extractDropdownValues(csvData)
        //TODO, write to database???
    }


    //TODO, does it make sense to save the rows of the csv to the database directly, or to the StudyArea data structs?

}

class HomeViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ObservationLogApplication
        return HomeViewModel(app, app.observationRepo) as T
    }
}