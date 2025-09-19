package weddellseal.markrecap.ui.home

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
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
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.ui.utils.getDeviceName
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

    private val context: Context
        get() = getApplication()

    internal val jobs = mutableJobSet()

    data class UiState(
        val overrideColony: Boolean = false,
        val latitudeDegrees: Int = -77,
        val latitudeDecimals: Int = 0,
        val longitudeDegrees: Int = 166,
        val longitudeDecimals: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _metadata = MutableStateFlow(ObservationMetadata(selectedColony = null))
    val metadata: StateFlow<ObservationMetadata> = _metadata.asStateFlow()

    // Initialize the ViewModel
    init {
        val deviceID = getDeviceName(context)
        val currentSeason = getCurrentYear().toString()

        _metadata.update {
            it.copy(
                deviceID = deviceID,
                currentSeason = currentSeason
            )
        }
    }

    val observersList: StateFlow<List<String>> = observersRepository.observersList
        .stateIn(viewModelScope, SharingStarted.Companion.Lazily, emptyList())

    val coloniesList: StateFlow<List<String>> = sealColonyRepository.coloniesList
        .stateIn(viewModelScope, SharingStarted.Companion.Lazily, emptyList())

    // Auto-detected colony
    private val _autoDetectedColony = MutableStateFlow<SealColony?>(null)
    val autoDetectedColony: StateFlow<SealColony?> = _autoDetectedColony

    fun setAutoDetectedColony(colony: SealColony?) {
        _autoDetectedColony.value = colony
    }

    // User Selection for Colony
    fun setManualColonyCheckbox(value: Boolean) {
        _uiState.update { it.copy(overrideColony = value) }
    }

    fun updateOtherColonyLatitude(value: String) {
        val intVal = value.toIntOrNull()
        if (intVal != null) {
            _uiState.update { it.copy(latitudeDecimals = intVal) }
        }
    }

    fun clearOtherColonyLatitude() {
        _uiState.update { it.copy(latitudeDecimals = 0) }
    }

    fun updateOtherColonyLongitude(value: String) {
        val intVal = value.toIntOrNull()
        if (intVal != null) {
            _uiState.update { it.copy(longitudeDecimals = intVal) }
        }
    }

    fun clearOtherColonyLongitude() {
        _uiState.update { it.copy(longitudeDecimals = 0) }
    }


    fun clearColony() {
        _metadata.update { it.copy(selectedColony = null) }
    }

    fun updateSelectedColony(observationSiteSelected: String) {
        // lookup coordinates of selected colony, null if not found
        viewModelScope.launch {
            _metadata.update { it.copy(selectedColony = findColonyByName(observationSiteSelected)) }
        }
    }

    // User Selection for Observers
    fun updateObserversSelection(selected: List<String>) {
        val updated = selected.ifEmpty { emptyList() }
        _metadata.update { it.copy(selectedObservers = updated) }
    }

    // User Selections for Census
    fun updateCensusNumber(censusNumber: String) {
        _metadata.update { it.copy(censusNumber = censusNumber) }
    }

    fun updateIsCensusMode(observationMode: Boolean) {
        _metadata.update { it.copy(isCensusMode = observationMode) }
    }

    fun clearCensus() {
        _metadata.update { it.copy(censusNumber = "", isCensusMode = false) }
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

    // Location following
    var isFollowingLocation = mutableStateOf(false)
    private var lastKnownCoordinates: Coordinates? = null
    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: StateFlow<GeoLocation?> = _currentLocation

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

    fun hasPreciseLocation(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun applyLocationFollowing(isEnabled: Boolean) {
        Log.i(TAG, "follow location -> $isEnabled")
        isFollowingLocation.value = isEnabled
    }


    private fun configureLocationFollow() {
        viewModelScope.launch {
            Log.i(TAG, "observing location follow mode")

            locationSource.locationUpdates().collect { geoLocation ->
                // Check if the coordinates have changed, exit early if they remain unchanged
                if (geoLocation.coordinates == lastKnownCoordinates) {
//                    Log.i(TAG, "Location unchanged: ${geoLocation.coordinates.latitude}, ${geoLocation.coordinates.longitude}")
                    return@collect // Skip the update
                }

//                Log.i(TAG, "New location received: ${geoLocation.coordinates.latitude}, ${geoLocation.coordinates.longitude}")

                // Update the last known coordinates with the new coordinates
                lastKnownCoordinates = geoLocation.coordinates
                _currentLocation.value = geoLocation

//                Log.d(TAG, "${uiState.value.manualColonyCheckbox}")

                // Find and update the colony based on the new coordinates
                val colony = findColony(geoLocation.coordinates) ?: SealColony(
                    colonyId = 0,
                    inOut = "none",
                    location = "Seal colony not detected",
                    nLimit = 0.0,
                    sLimit = 0.0,
                    wLimit = 0.0,
                    eLimit = 0.0,
                    adjLong = 0.0,
                    adjLat = 0.0,
                    fileUploadId = 0
                )

                // update the auto-detected colony
                setAutoDetectedColony(colony)
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

    suspend fun findColonyByName(colonyName: String): SealColony? {
        return withContext(Dispatchers.IO) {
            sealColonyRepository.findColonyByName(colonyName)
        }
    }

    // This uses coordinates from the auto-detected colony or
    // from a colony that the user selects, including "Other"
    // and then uses the coordinates that the user enters
    fun getColonyLocation(): GeoLocation? {
        val colony = metadata.value.selectedColony?.let {
            if (it.location == "Other") {
                val lat =
                    uiState.value.latitudeDegrees + uiState.value.latitudeDecimals / 1000.0
                val long =
                    uiState.value.longitudeDegrees + uiState.value.longitudeDecimals / 1000.0

                GeoLocation(Coordinates(lat, long))
            } else {
                GeoLocation(Coordinates(it.adjLat, it.adjLong))
            }
        } ?: currentLocation
        return colony as GeoLocation?
    }
}