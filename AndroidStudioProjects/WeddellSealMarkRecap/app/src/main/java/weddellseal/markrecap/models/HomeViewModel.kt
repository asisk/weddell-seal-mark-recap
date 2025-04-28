package weddellseal.markrecap.models

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.room.SealColonyRepository
import weddellseal.markrecap.frameworks.room.SupportingDataRepository
import weddellseal.markrecap.frameworks.room.files.FailedRow
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.utils.mutableJobSet
import weddellseal.markrecap.ui.utils.storeIn
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
    private var lastKnownCoordinates: Coordinates? = null

    internal val jobs = mutableJobSet()

    private val _uiState = MutableStateFlow(
        UiState(
            date = SimpleDateFormat(
                "dd.MM.yyyy HH:mm:ss aaa z",
                Locale.US
            ).format(System.currentTimeMillis()),
        )
    )

    val uiState: StateFlow<UiState> = _uiState
    var isFollowingLocation = mutableStateOf(false)

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
//        val hasFileAccess: Boolean,
        val loading: Boolean = false,
        val isSaving: Boolean = false,
        var uriForCSVWrite: Uri? = null,
        var fileUploads: List<FileUploadEntity> = emptyList(),
        val failedColoniesRows: List<FailedRow> = emptyList(),
        val totalColoniesRows: Int = 0,
        val failedObserversRows: List<FailedRow> = emptyList(),
        val totalObserversRows: Int = 0,
        val isError: Boolean = false,
        val errorText: String = "",
        val date: String,
        val deviceCoordinates: Coordinates? = null,
        val location: GeoLocation? = null,
    )

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationSource.stopLocationUpdates()
        }
        jobs.clear()
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
                    Log.i(
                        TAG,
                        "Coordinates are the same as the previous update. Skipping update."
                    )
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

    //TODO, relocate this function
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
}