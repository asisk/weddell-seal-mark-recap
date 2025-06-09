package weddellseal.markrecap.models

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.utils.mutableJobSet
import weddellseal.markrecap.ui.utils.storeIn

private const val TAG = "HomeViewModel"

/*
 * Home Screen model
 */
class HomeViewModel(
    application: Application,
    private val locationSource: LocationSource,
    private val sealColonyRepository: SealColonyRepository,
    observersRepository: ObserversRepository,
) : AndroidViewModel(application) {

    internal val jobs = mutableJobSet()

    data class UiState(
        val isCensusMode: Boolean = false,
        val selectedCensusNumber: String = "",
        val selectedObservers: List<String> = listOf(),
        val selectedColony: String = "",
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val observersList: StateFlow<List<String>> = observersRepository.observersList
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val coloniesList: StateFlow<List<String>> = sealColonyRepository.coloniesList
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Auto-detected colony
    private val _autoDetectedColony = MutableStateFlow<SealColony?>(null)
    val autoDetectedColony: StateFlow<SealColony?> = _autoDetectedColony

    private val _overrideAutoColony = MutableStateFlow(false)
    val overrideAutoColony: StateFlow<Boolean> = _overrideAutoColony

    fun updateColony(colony: SealColony?) {
        _autoDetectedColony.value = colony
    }

    fun updateOverrideAutoColony(value: Boolean) {
        _overrideAutoColony.value = value
    }

    fun updateIsCensusMode(observationMode: Boolean) {
        _uiState.update{it.copy(isCensusMode = observationMode)}
    }

    fun clearCensus() {
        _uiState.update{it.copy(selectedCensusNumber = "", isCensusMode = false)}
    }

//    init {
//        simulateLocationForTesting() // <- temp test injection
//        // OR call configureLocationFollow() for real updates
//    }
//
//    private fun simulateLocationForTesting() {
//        viewModelScope.launch {
//            delay(3000)
//            _currentLocation.value = GeoLocation(
//                coordinates = Coordinates(42.0, -100.0),
//                updatedDate = "Fake Update"
//            )
//            Log.d("HomeViewModel", "Simulated location update emitted.")
//        }
//    }

    fun updateColonySelection(observationSiteSelected: String) {
        _uiState.update{it.copy(selectedColony = observationSiteSelected)}
    }

    fun updateObserversSelection(selected: List<String>) {
        val updated = if (selected.isEmpty()) emptyList() else selected
        _uiState.update { it.copy(selectedObservers = updated) }
    }

    fun updateCensusNumber(censusNumber: String) {
        _uiState.update{it.copy(selectedCensusNumber = censusNumber)}
    }

    // Location following
    var isFollowingLocation = mutableStateOf(false)
    private var lastKnownCoordinates: Coordinates? = null
    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: StateFlow<GeoLocation?> = _currentLocation.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.i(TAG, "onCleared: stopping location updates")
            locationSource.stopLocationUpdates()
        }
        jobs.clear()
    }

    // Location permissions
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

                // Check if the coordinates have changed
                if (geoLocation.coordinates == lastKnownCoordinates) {
                    Log.i(
                        TAG,
                        "Coordinates are the same as the previous update. Skipping update."
                    )
                    return@collect // Skip the update
                }

                Log.i(TAG, "New location received: $geoLocation")

                // Update the last known coordinates
                lastKnownCoordinates = geoLocation.coordinates
                Log.d("HomeViewModel", "Emitting location: $geoLocation")
                _currentLocation.value = geoLocation

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
            sealColonyRepository.findColony(
                coordinates.latitude,
                coordinates.longitude
            )
        }
    }
}