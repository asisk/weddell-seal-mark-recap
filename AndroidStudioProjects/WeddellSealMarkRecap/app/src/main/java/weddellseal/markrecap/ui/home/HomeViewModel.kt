package weddellseal.markrecap.ui.home

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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

    data class UiState(
        val isFollowingLocation: Boolean = false,
        val lastKnownCoordinates: Coordinates? = null,
        val overrideColony: Boolean = false,
        val latitudeDegrees: Int = -77,
        val latitudeDecimals: Int = 0,
        val longitudeDegrees: Int = 166,
        val longitudeDecimals: Int = 0
    )

    // STATEFLOWS
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _metadata = MutableStateFlow(ObservationMetadata(selectedColony = null))
    val metadata: StateFlow<ObservationMetadata> = _metadata.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: StateFlow<GeoLocation?> = _currentLocation

    private val _autoDetectedColony = MutableStateFlow<SealColony?>(null)
    val autoDetectedColony: StateFlow<SealColony?> = _autoDetectedColony

    val observersList: StateFlow<List<String>> = observersRepository.observersList
        .stateIn(viewModelScope, SharingStarted.Companion.Lazily, emptyList())

    val coloniesList: StateFlow<List<String>> =
        sealColonyRepository.coloniesList
            .map { list ->
                val (firstItem, rest) = list.partition { it == "Other" }
                // `firstItem` will be ["Other"] or empty
                firstItem + rest
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    // JOBS
    internal val jobs = mutableJobSet()
    // JOBS
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.i(TAG, "onCleared: stopping location updates")
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

        Log.i(TAG, "Location permissions granted, starting location updates")
        Log.i(TAG, "Current permissions status - Fine: ${hasPreciseLocation(context)}")

        viewModelScope.launch {
            applyLocationFollowing(true)
            locationSource.startLocationUpdates()
            Log.i(TAG, "Location updates initiated")
        }.storeIn(jobs)
    }

    private fun configureLocationFollow() {
        viewModelScope.launch(Dispatchers.IO) { // Move to IO thread
            Log.i(TAG, "configureLocationFollow: Starting to observe location updates")

            try {
                locationSource.locationUpdates().collect { geoLocation ->
                    Log.i(TAG, "configureLocationFollow: new latitude ${geoLocation.coordinates.latitude}")
                    // Update UI state with new coordinates (StateFlow updates are thread-safe)
                    _uiState.update { it.copy(lastKnownCoordinates = geoLocation.coordinates) }
                    _currentLocation.value = geoLocation
                    Log.d(TAG, "configureLocationFollow: Successfully updated location")
                }
            } catch (e: Exception) {
                Log.e(TAG, "configureLocationFollow: Error in location collection", e)
                // Restart location collection after a delay
                delay(2000)
                Log.i(TAG, "configureLocationFollow: Restarting location collection")
                configureLocationFollow() // Recursive restart
            }
        }.storeIn(jobs)
    }

    private fun observeColonyUpdates() {
        viewModelScope.launch(Dispatchers.IO) { // Move to IO thread
            currentLocation
                .filterNotNull()
                .collect { geoLocation ->
                    updateColonyForLocation(geoLocation)
                }
        }.storeIn(jobs)
    }

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

        // Launch both collectors once in init {} and let them idle until data arrives.
        // In onPermissionsResult only toggle start/stopLocationUpdates().
        // This addresses the risk of duplicate jobs in the case onPermissionsResult is called more than once.
        configureLocationFollow()
        observeColonyUpdates()
    }

    // User Selection for Observers
    fun updateObserversSelection(selected: List<String>) {
        val updated = selected.ifEmpty { emptyList() }
        _metadata.update { it.copy(selectedObservers = updated) }
    }

    private fun applyLocationFollowing(isEnabled: Boolean) {
        Log.i(TAG, "follow location -> $isEnabled")
        _uiState.update { it.copy(isFollowingLocation = true) }
    }

    fun setAutoDetectedColony(colony: SealColony?) {
        _autoDetectedColony.value = colony
    }

    // User Selection for Colony
    fun setOverrideColonyCheckbox(value: Boolean) {
        _uiState.update { it.copy(overrideColony = value) }
    }

    fun updateSelectedColony(observationSiteSelected: String) {
        // lookup coordinates of selected colony, null if not found
        viewModelScope.launch {
            _metadata.update { it.copy(selectedColony = findColonyByName(observationSiteSelected)) }
        }
    }

    // User Selected Colony Other, to facilitate entering latitude and longitude manually
    fun updateOtherColonyLatitude(value: String) {
        val intVal = value.toIntOrNull()
        if (intVal != null) {
            _uiState.update { it.copy(latitudeDecimals = intVal) }
        }
    }

    fun updateOtherColonyLongitude(value: String) {
        val intVal = value.toIntOrNull()
        if (intVal != null) {
            _uiState.update { it.copy(longitudeDecimals = intVal) }
        }
    }

    fun clearColony() {
        _metadata.update { it.copy(selectedColony = null) }
    }

    fun clearOtherColonyLatitude() {
        _uiState.update { it.copy(latitudeDecimals = 0) }
    }

    fun clearOtherColonyLongitude() {
        _uiState.update { it.copy(longitudeDecimals = 0) }
    }

    private suspend fun updateColonyForLocation(geoLocation: GeoLocation) {
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

        setAutoDetectedColony(colony)
    }

    // get the colony by coordinates
    suspend fun findColony(coordinates: Coordinates): SealColony? {
        return withContext(Dispatchers.IO) {
            sealColonyRepository.findColony(
                coordinates.latitude,
                coordinates.longitude
            )
        }
    }

    // get the colony by name
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
                    uiState.value.latitudeDegrees + uiState.value.latitudeDecimals / 100000.0
                val long =
                    uiState.value.longitudeDegrees + uiState.value.longitudeDecimals / 100000.0

                GeoLocation(Coordinates(lat, long))
            } else {
                GeoLocation(Coordinates(it.adjLat, it.adjLong))
            }
        } ?: currentLocation.value
        return colony
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

    fun hasPreciseLocation(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}