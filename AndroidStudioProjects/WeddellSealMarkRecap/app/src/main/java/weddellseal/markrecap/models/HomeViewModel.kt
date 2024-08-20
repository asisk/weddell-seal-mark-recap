package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.Observers
import weddellseal.markrecap.data.SealColony
import weddellseal.markrecap.data.SupportingDataRepository
import java.io.IOException
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

    // MutableStateFlow to hold the list of observers
    private val _observers = MutableStateFlow<List<String>>(emptyList())
    val observers: StateFlow<List<String>> = _observers

    // MutableStateFlow to hold the list of locations
    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val colonies: StateFlow<List<String>> = _locations

    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = false,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        val failedColoniesRows: List<String> = emptyList(),
        val totalColoniesRows: Int = 0,
        val failedObserversRows: List<String> = emptyList(),
        val totalObserversRows: Int = 0,
        val isColonyLocationsLoading: Boolean = false,
        val isColonyLocationsLoaded: Boolean = false,
        val lastColoniesFileNameLoaded: String = "",
        val isObserversLoading: Boolean = false,
        val isObserversLoaded: Boolean = false,
        val lastObserversFileNameLoaded: String = "",
        val isError: Boolean = false,
        val date: String,
    )

    init {
        fetchLocations()
        fetchObservers()
    }

    // Function to fetch locations from the database
    fun fetchLocations() {
        viewModelScope.launch {
            val fetchedLocations = withContext(Dispatchers.IO) {
                supportingDataRepository.getLocations() // Fetch from DB
            }
            if (fetchedLocations.isNotEmpty() && _locations.value.isEmpty()) {
                _locations.value = fetchedLocations
            }
        }
    }

    // Function to fetch locations from the database
    fun fetchObservers() {
        viewModelScope.launch {
            val fetchedObservers = withContext(Dispatchers.IO) {
                supportingDataRepository.getObserverInitials() // Fetch from DB
            }
            if (fetchedObservers.isNotEmpty() && _observers.value.isEmpty()) {
                _observers.value = fetchedObservers
            }
        }
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
                _uiState.value = uiState.value.copy(hasFileAccess = isGranted)
            }

            else -> {
                Log.e("Permission change", "Unexpected permission: $permission")
            }
        }
    }

    private fun readSealColoniesCsvData(
        contentResolver: ContentResolver,
        uri: Uri
    ): Pair<List<SealColony>, List<String>> {
        val csvData: MutableList<SealColony> = mutableListOf()
        val failedRows = mutableListOf<String>()

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val inOutIndex = headerRow.indexOf("In/Out")
                val locationIndex = headerRow.indexOf("Location")
                val nLimitIndex = headerRow.indexOf("N_Limit")
                val sLimitIndex = headerRow.indexOf("S_Limit")
                val wLimitIndex = headerRow.indexOf("W_Limit")
                val eLimitIndex = headerRow.indexOf("E_Limit")
                val adjLatIndex = headerRow.indexOf("Adj_Lat")
                val adjLongIndex = headerRow.indexOf("Adj_Long")

                if (listOf(
                        inOutIndex,
                        locationIndex,
                        nLimitIndex,
                        sLimitIndex,
                        wLimitIndex,
                        eLimitIndex,
                        adjLatIndex,
                        adjLongIndex
                    ).all { it != -1 }
                ) {
                    reader.forEachLine { line ->
                        try {
                            val row = line.split(",")
                            val inOut = row.getOrNull(inOutIndex) ?: ""
                            val location = row.getOrNull(locationIndex) ?: ""
                            val nLimit = row.getOrNull(nLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val sLimit = row.getOrNull(sLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val wLimit = row.getOrNull(wLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val eLimit = row.getOrNull(eLimitIndex)?.toDoubleOrNull() ?: 0.0
                            val adjLat = row.getOrNull(adjLatIndex)?.toDoubleOrNull() ?: 0.0
                            val adjLong = row.getOrNull(adjLongIndex)?.toDoubleOrNull() ?: 0.0

                            val record = SealColony(
                                colonyId = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                                inOut = inOut,
                                location = location,
                                nLimit = nLimit,
                                sLimit = sLimit,
                                wLimit = wLimit,
                                eLimit = eLimit,
                                adjLat = adjLat,
                                adjLong = adjLong
                            )

                            // Add the parsed entity to the list
                            csvData.add(record)

                        } catch (e: Exception) {
                            // Handle and log any errors in parsing the row
                            e.printStackTrace()
                            // Capture the raw row that failed
                            failedRows.add(line)
                        }
                    }
                } else {
                    // one or more headers are missing
                    throw IllegalArgumentException("CSV file missing required headers")
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }

    fun loadSealColoniesFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(loading = true, isColonyLocationsLoading = true)

            try {

                // Read CSV data on IO dispatcher
                val (csvData, failedRows) = withContext(Dispatchers.IO) {
                    readSealColoniesCsvData(context.contentResolver, uri)
                }

                _uiState.value = uiState.value.copy(
                    loading = false,
                    isColonyLocationsLoading = false,
                    isColonyLocationsLoaded = true,
                    totalColoniesRows = csvData.size + failedRows.size,
                    isError = failedRows.isNotEmpty(),
                    failedColoniesRows = failedRows
                )

                // Insert CSV data into the database
                supportingDataRepository.insertColoniesData(csvData)

            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    loading = false,
                    isColonyLocationsLoading = false,
                    isColonyLocationsLoaded = false,
                    totalColoniesRows = 0,
                    isError = true
                )
            }
        }
    }

    fun updateLastColoniesFileNameLoaded(fileName: String) {
        _uiState.value = uiState.value.copy(
            lastColoniesFileNameLoaded = fileName
        )
    }

    fun loadObserversFile(uri: Uri) {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(loading = true, isObserversLoading = true)

            try {

                // Read CSV data on IO dispatcher
                val (csvData, failedRows) = withContext(Dispatchers.IO) {
                    readObserverCsvData(context.contentResolver, uri)
                }
                _uiState.value = uiState.value.copy(
                    loading = false,
                    isObserversLoading = false,
                    isObserversLoaded = true,
                    totalObserversRows = csvData.size + failedRows.size,
                    isError = failedRows.isNotEmpty(),
                    failedObserversRows = failedRows
                )

                // Insert CSV data into the database
                supportingDataRepository.insertObserversData(csvData)

            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    loading = false,
                    isObserversLoading = false,
                    isObserversLoaded = false,
                    isError = true,
                    totalObserversRows = 0
                )
            }
        }
    }

    fun updateLastObserversFileNameLoaded(fileName: String) {
        _uiState.value = uiState.value.copy(
            lastObserversFileNameLoaded = fileName
        )
    }

    private fun readObserverCsvData(
        contentResolver: ContentResolver,
        uri: Uri,
    ): Pair<List<Observers>, List<String>> {
        val csvData: MutableList<Observers> = mutableListOf()
        val failedRows = mutableListOf<String>()

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val initialsIndex = headerRow.indexOf("Initials")

                if (initialsIndex != -1) {
                    reader.forEachLine { line ->

                        try {

                            val row = line.split(",")
                            val observer = row.getOrNull(initialsIndex) ?: ""
                            val record = Observers(
                                observerId = 0,
                                initials = observer
                            )
                            csvData.add(record)

                        } catch (e: Exception) {
                            // Handle and log any errors in parsing the row
                            e.printStackTrace()
                            // Capture the raw row that failed
                            failedRows.add(line)
                        }
                    }
                } else {
                    // one or more headers are missing
                    throw IllegalArgumentException("CSV file missing required headers")
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }
}