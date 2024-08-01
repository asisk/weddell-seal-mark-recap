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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.Observers
import weddellseal.markrecap.data.SealColony
import weddellseal.markrecap.data.SupportingDataRepository
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Home Screen model
 */
class HomeViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val supportingDataRepository: SupportingDataRepository
) : AndroidViewModel(application) {
    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = true,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        var colonyLocations: List<String> = emptyList(),
        var observerInitials: List<String> = emptyList(),
        val date: String,
    )

    private val context: Context
        get() = getApplication()
    var uiState by mutableStateOf(UiState(
        hasFileAccess = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
        date = SimpleDateFormat(
            "dd.MM.yyyy HH:mm:ss aaa z",
            Locale.US
        ).format(System.currentTimeMillis()),
    ))
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val colonyLocations = withContext(Dispatchers.IO) { supportingDataRepository.getLocations() }
            val observerInitials = withContext(Dispatchers.IO) { supportingDataRepository.getObserverInitials() }
            uiState = uiState.copy(
                colonyLocations = colonyLocations,
                observerInitials = observerInitials
            )
        }
    }

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

    private fun readSealColoniesCsvData(
        contentResolver: ContentResolver,
        uri: Uri
    ): List<SealColony> {
        val sealColonies: MutableList<SealColony> = mutableListOf()
        val dropdownList = mutableListOf<String>()
        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->
                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val inOut = headerRow.indexOf("InOut")
                val locationIndex = headerRow.indexOf("Location")
                val nLimit = headerRow.indexOf("nLimit")
                val sLimit = headerRow.indexOf("sLimit")
                val wLimit = headerRow.indexOf("wLimit")
                val eLimit = headerRow.indexOf("eLimit")
                val adjLat = headerRow.indexOf("Adj_Lat")
                val adjLong = headerRow.indexOf("Adj_Long")

                reader.forEachLine { line ->
                    val row = line.split(",")
                    if (row.size >= 3 && locationIndex != -1 && adjLat != -1 && adjLong != -1) {
                        val inOut = row.getOrNull(locationIndex) ?: ""

                        val location = row.getOrNull(locationIndex) ?: ""
                        dropdownList.add(location)

                        val nLimit = row.getOrNull(nLimit)?.toDoubleOrNull() ?: 0.0
                        val sLimit = row.getOrNull(sLimit)?.toDoubleOrNull() ?: 0.0
                        val wLimit = row.getOrNull(wLimit)?.toDoubleOrNull() ?: 0.0
                        val eLimit = row.getOrNull(eLimit)?.toDoubleOrNull() ?: 0.0
                        val latitude = row.getOrNull(adjLat)?.toDoubleOrNull() ?: 0.0
                        val longitude = row.getOrNull(adjLong)?.toDoubleOrNull() ?: 0.0

                        val record = SealColony(
                            colonyId = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                            inOut = inOut,
                            location = location,
                            nLimit = nLimit,
                            sLimit = sLimit,
                            wLimit = wLimit,
                            eLimit = eLimit,
                            adjLat = latitude,
                            adjLong = longitude
                        )

                        // Add the parsed entity to the list
                        sealColonies.add(record)
                    } else {
                        // Handle invalid row or missing columns
                    }
                }
                uiState = uiState.copy(colonyLocations = dropdownList)
            }
        }

        return sealColonies
    }

    fun loadSealColoniesFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            // Read CSV data on IO dispatcher
            val csvData = withContext(Dispatchers.IO) {
                readSealColoniesCsvData(context.contentResolver, uri)
            }

            // Insert CSV data into the database
            supportingDataRepository.insertColoniesData(csvData)
        }
    }

    fun loadObserversFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            // Read CSV data on IO dispatcher
            val csvData = withContext(Dispatchers.IO) {
                readObserverCsvData(context.contentResolver, uri)
            }

            // Insert CSV data into the database
            supportingDataRepository.insertObserversData(csvData)
        }
    }


    private fun readObserverCsvData(contentResolver: ContentResolver, uri: Uri): List<Observers> {
        val observers: MutableList<Observers> = mutableListOf()
        val dropdownList = mutableListOf<String>()
        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->
                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val initialsIndex = headerRow.indexOf("ObserverInitials")

                reader.forEachLine { line ->
                    val row = line.split(",")
                    if (row.isNotEmpty() && initialsIndex != -1) {
                        val observer = row.getOrNull(initialsIndex) ?: ""
                        dropdownList.add(observer.toString())

                        val record = Observers(
                            observerId = 0,
                            initials = observer
                        )

                        observers.add(record)
                    } else {
                        // Handle invalid row or missing columns
                    }
                }
                uiState = uiState.copy(observerInitials = dropdownList)
            }
        }

        return observers
    }
}