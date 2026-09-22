package weddellseal.markrecap.ui.home

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.location.isAccurateEnoughForColonyMiss
import weddellseal.markrecap.domain.location.shouldUseIncomingLocation
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
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
        val isRefreshingGps: Boolean = false,
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

    // Guard to prevent multiple location collection coroutines
    private var isLocationCollectionActive = false

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
        // viewModelScope is already cancelled here; stop GPS on this thread.
        Log.i(TAG, "onCleared: stopping location updates")
        locationSource.stopLocationUpdates()
        jobs.clear()
        isLocationCollectionActive = false
    }

    fun onPermissionsResult(granted: Boolean) {
        Log.i(TAG, "onPermissionsResult: $granted")

        if (!granted) {
            Log.e(TAG, "Location permissions denied!")
            locationSource.stopLocationUpdates()
            applyLocationFollowing(false)
            return
        }

        Log.i(TAG, "Location permissions granted, starting location updates")
        Log.i(TAG, "Current permissions status - Fine: ${hasPreciseLocation(context)}")

        applyLocationFollowing(true)
        locationSource.startLocationUpdates()
        configureLocationFollow()
        Log.i(TAG, "Location updates initiated")
    }

    private fun configureLocationFollow() {
        // Prevent multiple location collection coroutines
        if (isLocationCollectionActive) {
            Log.w(TAG, "configureLocationFollow: Already active, skipping duplicate call")
            return
        }
        
        isLocationCollectionActive = true
        viewModelScope.launch {
            try {
                locationSource.locationUpdates().collect { geoLocation ->
                    applyIncomingLocation(geoLocation)
                }
            } catch (e: Exception) {
                Log.e(TAG, "configureLocationFollow: Error in location collection", e)
                isLocationCollectionActive = false
                // Restart location collection after a delay
                delay(2000)
                configureLocationFollow() // Recursive restart
            }
        }.storeIn(jobs)
    }

    private fun applyIncomingLocation(geoLocation: GeoLocation) {
        val currentIsLive = _currentLocation.value?.isLiveFix == true
        if (!shouldUseIncomingLocation(currentIsLive, geoLocation.isLiveFix)) {
            return
        }
        _uiState.update { it.copy(lastKnownCoordinates = geoLocation.coordinates) }
        _currentLocation.value = geoLocation
    }

    /**
     * One-shot high-accuracy refresh (fresh fix only). Does not change the continuous
     * acquire/track cadence; use when a technician wants an immediate update.
     *
     * Clears displayed coordinates (and auto-detected colony when not overriding) while
     * the request is in flight so Refresh feels like an update even when the new fix
     * matches the old one.
     */
    fun refreshGps() {
        if (_uiState.value.isRefreshingGps) return
        viewModelScope.launch {
            val previousLocation = _currentLocation.value
            val previousAutoColony = _autoDetectedColony.value
            val previousSelectedColony = metadata.value.selectedColony
            val clearingAutoColony = !_uiState.value.overrideColony

            _uiState.update { it.copy(isRefreshingGps = true) }
            _currentLocation.value = null
            if (clearingAutoColony) {
                setAutoDetectedColony(null)
                syncSelectedColonyFromAutoDetect()
            }
            try {
                locationSource.requestSingleUpdate()
                    .onSuccess { applyIncomingLocation(it) }
                    .onFailure { e ->
                        Log.w(TAG, "refreshGps: failed to get current location", e)
                        if (_currentLocation.value == null && previousLocation != null) {
                            _currentLocation.value = previousLocation
                        }
                        if (clearingAutoColony && _autoDetectedColony.value == null) {
                            setAutoDetectedColony(previousAutoColony)
                            if (!_uiState.value.overrideColony) {
                                _metadata.update { it.copy(selectedColony = previousSelectedColony) }
                            }
                        }
                    }
            } finally {
                _uiState.update { it.copy(isRefreshingGps = false) }
            }
        }.storeIn(jobs)
    }

    private fun observeColonyUpdates() {
        viewModelScope.launch {
            currentLocation
                .filterNotNull()
                .collect { geoLocation ->
                    updateColonyForLocation(geoLocation)
                }
        }.storeIn(jobs)
    }

    /**
     * Colony auto-detect only runs when location changes. If colonies are imported after a
     * live fix (common: empty DB at launch, then upload Baxter Meadows), re-query the boxes
     * against the current live position so Home / Save pick up the new catalog.
     */
    private fun observeColonyCatalogChanges() {
        viewModelScope.launch {
            sealColonyRepository.coloniesList.collect {
                reevaluateColonyForCurrentLocation()
            }
        }.storeIn(jobs)
    }

    private suspend fun reevaluateColonyForCurrentLocation() {
        val live = currentLocation.value?.takeIf { it.isLiveFix } ?: return
        updateColonyForLocation(live)
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

        // Only start colony observation - location collection starts after permissions are granted
        // This prevents trying to collect from a flow that isn't emitting yet
        observeColonyUpdates()
        observeColonyCatalogChanges()
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

    /** Leave override mode and restore the GPS-detected colony into metadata for save. */
    fun useGpsColony() {
        setOverrideColonyCheckbox(false)
        syncSelectedColonyFromAutoDetect()
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
        if (!geoLocation.isLiveFix) return

        val matched = findColony(geoLocation.coordinates)
        if (matched != null) {
            setAutoDetectedColony(matched)
            syncSelectedColonyFromAutoDetect()
            return
        }

        // A poor fix that misses every box is still "waiting", not "not detected".
        if (!isAccurateEnoughForColonyMiss(geoLocation.accuracyMeters)) {
            setAutoDetectedColony(null)
            syncSelectedColonyFromAutoDetect()
            return
        }

        setAutoDetectedColony(
            SealColony(
                colonyId = 0,
                inOut = "none",
                location = ColonyPopulation.NOT_DETECTED,
                nLimit = 0.0,
                sLimit = 0.0,
                wLimit = 0.0,
                eLimit = 0.0,
                adjLong = 0.0,
                adjLat = 0.0,
                fileUploadId = 0
            )
        )
        syncSelectedColonyFromAutoDetect()
    }

    /**
     * Keep [ObservationMetadata.selectedColony] in sync with GPS auto-detect when not
     * overriding. Save validation and CSV colony name read selectedColony; the Home row
     * displays autoDetectedColony — both must agree.
     */
    private fun syncSelectedColonyFromAutoDetect() {
        if (_uiState.value.overrideColony) return
        val detected = _autoDetectedColony.value
        val forSave = detected?.takeUnless {
            it.location == ColonyPopulation.NOT_DETECTED
        }
        _metadata.update { it.copy(selectedColony = forSave) }
    }

    // get the colony by coordinates
    suspend fun findColony(coordinates: Coordinates): SealColony? {
        return sealColonyRepository.findColony(
            coordinates.latitude,
            coordinates.longitude
        )
    }

    // get the colony by name
    suspend fun findColonyByName(colonyName: String): SealColony? {
        return sealColonyRepository.findColonyByName(colonyName)
    }

    /**
     * Coordinates written on Save.
     *
     * Override mode: use the hand-picked colony center, or manual Other decimals.
     * GPS mode: always the live device fix (never last-known, never colony-box center).
     */
    fun getColonyLocation(): GeoLocation? {
        if (_uiState.value.overrideColony) {
            val colony = metadata.value.selectedColony ?: return null
            return if (colony.location == "Other") {
                // Home UI shows "{degrees}." + up to 5 fractional digits (e.g. "-77." + "12345").
                // For negative degrees, subtract the fraction: -77 + 0.12345 = -76.87655 (wrong).
                val lat = composeManualCoordinate(
                    uiState.value.latitudeDegrees,
                    uiState.value.latitudeDecimals,
                )
                val long = composeManualCoordinate(
                    uiState.value.longitudeDegrees,
                    uiState.value.longitudeDecimals,
                )
                GeoLocation(Coordinates(lat, long))
            } else {
                GeoLocation(Coordinates(colony.adjLat, colony.adjLong))
            }
        }
        return currentLocation.value?.takeIf { it.isLiveFix }
    }

    /**
     * Builds a coordinate from the fixed degree label and typed fractional digits on the home
     * screen (max 5 digits → divide by 100000). Negative degrees must subtract the fraction so
     * the stored value matches the displayed "-77.xxxxx".
     */
    private fun composeManualCoordinate(degrees: Int, decimals: Int): Double {
        val fraction = decimals / 100000.0
        return if (degrees < 0) degrees - fraction else degrees + fraction
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