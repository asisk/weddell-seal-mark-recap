package weddellseal.markrecap.models

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.data.FailedRow
import weddellseal.markrecap.data.FileUploadEntity
import weddellseal.markrecap.data.ObservationRepository
import weddellseal.markrecap.data.Observers
import weddellseal.markrecap.data.SealColony
import weddellseal.markrecap.data.SealColonyRepository
import weddellseal.markrecap.data.SupportingDataRepository
import weddellseal.markrecap.data.location.Coordinates
import weddellseal.markrecap.data.location.GeoLocation
import weddellseal.markrecap.data.location.LocationSource
import weddellseal.markrecap.ui.utils.mutableJobSet
import weddellseal.markrecap.ui.utils.storeIn
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "HomeViewModel"

/*
 * Home Screen model
 */
class HomeViewModel(
    application: Application,
    private val observationRepo: ObservationRepository,
    private val supportingDataRepository: SupportingDataRepository,
    private val locationSource: LocationSource,
    private val sealColonyRepository: SealColonyRepository
) : AndroidViewModel(application) {
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted
    private var lastKnownCoordinates: Coordinates? = null

    internal val jobs = mutableJobSet()

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
    var isFollowingLocation = mutableStateOf(false)

    // Collect file upload data from the repository using collectAsState
    val fileUploads: StateFlow<List<FileUploadEntity>> = supportingDataRepository.fileUploads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Collect as StateFlow

    // MutableStateFlow to hold the list of observers
    private val _observers = MutableStateFlow<List<String>>(emptyList())
    val observers: StateFlow<List<String>> = _observers

    // MutableStateFlow to hold the list of locations
    private val _coloniesList = MutableStateFlow<List<String>>(emptyList())
    val coloniesList: StateFlow<List<String>> = _coloniesList

    // Colony that is auto-detected based on the location emitted from the FusedLocationSource
    val autoDetectedColony: StateFlow<SealColony?> = sealColonyRepository.colony

    fun updateColony(colony: SealColony?) {
        sealColonyRepository.setColony(colony)
    }

    // Expose the overrideAutoColony as a StateFlow
    val overrideAutoColony: StateFlow<Boolean> = sealColonyRepository.overrideAutoColony

    fun updateOverrideAutoColony(value: Boolean) {
        sealColonyRepository.setOverrideAutoColony(value)
    }

    private val _coordinates = MutableStateFlow<Coordinates?>(null)
    val coordinates: MutableStateFlow<Coordinates?> = _coordinates

    data class UiState(
        val hasFileAccess: Boolean,
        val loading: Boolean = false,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        var fileUploads: List<FileUploadEntity> = emptyList(),
        val failedColoniesRows: List<FailedRow> = emptyList(),
        val totalColoniesRows: Int = 0,
        val failedObserversRows: List<FailedRow> = emptyList(),
        val totalObserversRows: Int = 0,
        val isColonyLocationsLoading: Boolean = false,
        val isColonyLocationsLoaded: Boolean = false,
        val lastColoniesFileNameLoaded: String = "",
        val isObserversLoading: Boolean = false,
        val isObserversLoaded: Boolean = false,
        val lastObserversFileNameLoaded: String = "",
        val isError: Boolean = false,
        val errorText: String = "",
        val date: String,
        val deviceCoordinates: Coordinates? = null,
        val location: GeoLocation? = null,
    )

    data class UploadCardState(
        val fileType: String,
        val status: UploadStatus,
        val onUploadClick: () -> Unit
    )

    sealed class UploadStatus(val message: String, val color: Color) {
        object Success : UploadStatus("Upload successful", Color(0xFF2E7D32))
        data class Error(val error: String) : UploadStatus("Upload failed: $error", Color(0xFFC62828))
        object Idle : UploadStatus("", Color.Unspecified)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationSource.stopLocationUpdates()
        }
        jobs.clear()
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionChange(permission: String, isGranted: Boolean) {
        Log.i(TAG, "permissions changed")
        when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                Log.i(TAG, "READ_EXTERNAL_STORAGE changed to granted")

                _uiState.value = uiState.value.copy(hasFileAccess = isGranted)
            }

            else -> {
                Log.e(TAG, "Unexpected permission: $permission")
            }
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        Log.i(TAG, "onPermissionsResult: $granted")

        if (!granted) {
            Log.e(TAG, "Location permissions denied!")
            viewModelScope.launch {
                locationSource.stopLocationUpdates()
                applyLocationFollowing(false)
            }.storeIn(jobs)
            return
        }

        Log.i(TAG, "Location permissions granted, proceed with observing location changes")

        viewModelScope.launch {
            applyLocationFollowing(true)
            configureLocationFollow()
            locationSource.startLocationUpdates()
        }.storeIn(jobs)
    }

    private fun applyLocationFollowing(isEnabled: Boolean) {
        Log.i(TAG, "follow location -> $isEnabled")
        isFollowingLocation.value = isEnabled
    }

    private fun configureLocationFollow() {
        viewModelScope.launch {
            Log.i(TAG, "observing location follow mode")

            locationSource.locationUpdates().collect { geoLocation ->
                Log.i(TAG, "New location received: $geoLocation")

                // Check if the coordinates have changed
                if (geoLocation.coordinates == lastKnownCoordinates) {
                    Log.i(TAG, "Coordinates are the same as the previous update. Skipping update.")
                    return@collect // Skip the update
                }

                // Update the last known coordinates
                lastKnownCoordinates = geoLocation.coordinates

                _coordinates.value = geoLocation.coordinates

                _uiState.value = uiState.value.copy(
                    location = geoLocation // Update with the latest location
                )


                var sealColonyDefault = SealColony(
                    colonyId = 0,
                    inOut = "none",
                    location = "Seal Colony not Found!",
                    nLimit = 0.0,
                    sLimit = 0.0,
                    wLimit = 0.0,
                    eLimit = 0.0,
                    adjLong = 0.0,
                    adjLat = 0.0,
                    fileUploadId = 0
                )
                // Find and update the colony based on the location
                val colony = findColony(geoLocation.coordinates) ?: sealColonyDefault
                updateColony(colony)
            }
        }.storeIn(jobs)
    }

    // locate the colony name by querying the database
    suspend fun findColony(coordinates: Coordinates): SealColony? {
        return withContext(Dispatchers.IO) {
            supportingDataRepository.findColony(
                coordinates.latitude,
                coordinates.longitude
            )
        }
    }

    // Function to fetch locations from the database
    fun fetchColonyNamesList() {
        viewModelScope.launch {
            val fetchedLocations = withContext(Dispatchers.IO) {
                supportingDataRepository.getColonyNamesList()
            }
            if (fetchedLocations.isNotEmpty() && _coloniesList.value.isEmpty()) {
                _coloniesList.value = fetchedLocations
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

    fun loadSealColoniesFile(uri: Uri, filename: String) {
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(loading = true, isColonyLocationsLoading = true)

            try {
                // Insert the file and get the fileUploadId
                val fileUploadId = insertFileUpload(filename)

                // Read and process the CSV data
                val (csvData, failedRows) = readAndProcessColonyCsv(uri, fileUploadId)

                // Insert the CSV data into the database
                val insertedCount = insertColonyData(fileUploadId, csvData)

                // Update the file status based on success or failure
                supportingDataRepository.updateFileUploadStatus(
                    fileUploadId,
                    if (insertedCount > 0) "successful" else "failed"
                )

                // Update the UI state based on the result
                updateUiStateColonies(insertedCount, failedRows)

            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    loading = false,
                    isColonyLocationsLoading = false,
                    isColonyLocationsLoaded = false,
                    isError = true,
                    totalColoniesRows = 0
                )
            }
        }
    }

    fun loadObserversFile(uri: Uri, filename: String) {
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(loading = true, isObserversLoading = true)

            try {
                // Insert the file and get the fileUploadId
                val fileUploadId = insertFileUpload(filename)

                // Read and process the CSV data
                val (csvData, failedRows) = readAndProcessObserversCsv(uri, fileUploadId)

                // Insert the CSV data into the database
                val insertedCount = insertObserversData(fileUploadId, csvData)

                // Update the file status based on success or failure
                supportingDataRepository.updateFileUploadStatus(
                    fileUploadId,
                    if (insertedCount > 0) "successful" else "failed"
                )

                // Update the UI state based on the result
                updateUiStateObservers(insertedCount, failedRows)

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

    fun clearObservations() {
        viewModelScope.launch {
            try {
                observationRepo.softDeleteAllObservations()
            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    isError = true,
                    errorText = "Error removing observations"
                )
            }
        }
    }

    fun clearObservers() {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            try {
                supportingDataRepository.clearObserversData()
            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    isError = true,
                    errorText = "Error removing observers"
                )
            }
        }
    }

    fun clearColonies() {
        // Kick off this process on a coroutine
        viewModelScope.launch {
            try {
                supportingDataRepository.clearColonyData()
            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    isError = true,
                    errorText = "Error removing colonies"
                )
            }
        }
    }

    private suspend fun insertFileUpload(filename: String): Long {
        return supportingDataRepository.insertFileUpload(
            FileUploadEntity(
                id = 0,
                filename = filename,
                status = "pending"
            )
        )
    }

    private suspend fun readAndProcessObserversCsv(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<Observers>, List<FailedRow>> {
        return withContext(Dispatchers.IO) {
            readObserverCsvData(context.contentResolver, uri, fileUploadId)
        }
    }

    private suspend fun readAndProcessColonyCsv(
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<SealColony>, List<FailedRow>> {
        return withContext(Dispatchers.IO) {
            readSealColoniesCsvData(context.contentResolver, uri, fileUploadId)
        }
    }

    private suspend fun insertObserversData(fileUploadId: Long, csvData: List<Observers>): Int {
        return supportingDataRepository.insertObserversData(fileUploadId, csvData)
    }

    private suspend fun insertColonyData(fileUploadId: Long, csvData: List<SealColony>): Int {
        return supportingDataRepository.insertColoniesData(fileUploadId, csvData)
    }

    private fun updateUiStateObservers(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value =
            uiState.value.copy(
                loading = false,
                isObserversLoading = false,
                isObserversLoaded = true,
                totalObserversRows = insertedCount,
                isError = failedRows.isNotEmpty(),
                failedObserversRows = failedRows
            )
    }

    private fun updateUiStateColonies(insertedCount: Int, failedRows: List<FailedRow>) {
        _uiState.value =
            uiState.value.copy(
                loading = false,
                isColonyLocationsLoading = false,
                isColonyLocationsLoaded = true,
                totalColoniesRows = insertedCount,
                isError = failedRows.isNotEmpty(),
                failedColoniesRows = failedRows
            )
    }

    fun updateLastObserversFileNameLoaded(fileName: String) {
        _uiState.value = uiState.value.copy(
            lastObserversFileNameLoaded = fileName
        )
    }

    fun updateLastColoniesFileNameLoaded(fileName: String) {
        _uiState.value = uiState.value.copy(
            lastColoniesFileNameLoaded = fileName
        )
    }

    private fun readSealColoniesCsvData(
        contentResolver: ContentResolver,
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<SealColony>, List<FailedRow>> {
        val csvData: MutableList<SealColony> = mutableListOf()
        val failedRows = mutableListOf<FailedRow>()
        var lineNumber = 0

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()

                // Define the required headers and their corresponding column names
                val inOutIndex = headerRow.indexOf("In/Out")
                val locationIndex = headerRow.indexOf("Location")
                val nLimitIndex = headerRow.indexOf("N_Limit")
                val sLimitIndex = headerRow.indexOf("S_Limit")
                val wLimitIndex = headerRow.indexOf("W_Limit")
                val eLimitIndex = headerRow.indexOf("E_Limit")
                val adjLatIndex = headerRow.indexOf("Adj_Lat")
                val adjLongIndex = headerRow.indexOf("Adj_Long")

                val requiredHeaders = listOf(
                    inOutIndex,
                    locationIndex,
                    nLimitIndex,
                    sLimitIndex,
                    wLimitIndex,
                    eLimitIndex,
                    adjLatIndex,
                    adjLongIndex
                )

                // Check if any required headers are missing
                if (!requiredHeaders.all { it != -1 }) {
                    // Handle missing headers (e.g., throw an error, log, or show a message to the user)
                    failedRows.add(
                        FailedRow(
                            rowNumber = 0,
                            errorMessage = "CSV file missing required headers"
                        )
                    )
                    return Pair(csvData, failedRows)
                }

                reader.forEachLine { line ->
                    lineNumber++ // Increment line number for each iteration
                    try {
                        val row = line.split(",")

                        val record = SealColony(
                            colonyId = 0, // Room will autopopulate, pass zero only to satisfy the instantiation of the WedCheckRecord
                            inOut = row.getOrNull(inOutIndex) ?: "",
                            location = row.getOrNull(locationIndex) ?: "",
                            nLimit = row.getOrNull(nLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            sLimit = row.getOrNull(sLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            wLimit = row.getOrNull(wLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            eLimit = row.getOrNull(eLimitIndex)?.toDoubleOrNull() ?: 0.0,
                            adjLat = row.getOrNull(adjLatIndex)?.toDoubleOrNull() ?: 0.0,
                            adjLong = row.getOrNull(adjLongIndex)?.toDoubleOrNull() ?: 0.0,
                            fileUploadId = fileUploadId // Foreign key reference
                        )

                        // Add the parsed entity to the list
                        csvData.add(record)

                    } catch (e: Exception) {
                        // Handle and log any errors in parsing the row
                        e.printStackTrace()
                        // Capture the raw row that failed
                        FailedRow(
                            rowNumber = lineNumber,
                            errorMessage = "Error in row $lineNumber: ${e.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }

    private fun readObserverCsvData(
        contentResolver: ContentResolver,
        uri: Uri,
        fileUploadId: Long
    ): Pair<List<Observers>, List<FailedRow>> {
        val csvData: MutableList<Observers> = mutableListOf()
        val failedRows = mutableListOf<FailedRow>()
        var lineNumber = 0

        contentResolver.openInputStream(uri)?.use { stream ->
            InputStreamReader(stream).buffered().use { reader ->

                val headerRow = reader.readLine()?.split(",") ?: emptyList()
                val initialsIndex = headerRow.indexOf("Initials")

                if (initialsIndex != -1) {
                    reader.forEachLine { line ->
                        lineNumber++ // Increment line number for each iteration

                        try {

                            val row = line.split(",")
                            val record = Observers(
                                observerId = 0,
                                initials = row.getOrNull(initialsIndex) ?: "",
                                fileUploadId = fileUploadId // Foreign key reference
                            )
                            csvData.add(record)

                        } catch (e: Exception) {
                            // Handle and log any errors in parsing the row
                            e.printStackTrace()
                            // Capture the raw row that failed
                            failedRows.add(
                                FailedRow(
                                    rowNumber = lineNumber,
                                    errorMessage = "Invalid number format in row $lineNumber: ${e.localizedMessage}"
                                )
                            )
                        }
                    }
                } else {
                    // one or more headers are missing
                    failedRows.add(
                        FailedRow(
                            rowNumber = 0,
                            errorMessage = "CSV file missing required headers"
                        )
                    )
                    return Pair(csvData, failedRows)
                }
            }
        } ?: throw IOException("Unable to open input stream")

        return Pair(csvData, failedRows)
    }
}