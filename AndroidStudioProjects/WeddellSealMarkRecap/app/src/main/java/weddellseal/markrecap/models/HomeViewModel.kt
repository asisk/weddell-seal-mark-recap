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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
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
    private val observationRepo: ObservationRepository,
    private val locationSource: LocationSource,
    private val sealColonyRepository: SealColonyRepository,
    private val observersRepository: ObserversRepository,
) : AndroidViewModel(application) {

    internal val jobs = mutableJobSet()

//    data class UiState(
//        val hasFileAccess: Boolean,
//        val loading: Boolean = false,
//        val isSaving: Boolean = false,
//        var uriForCSVWrite: Uri? = null,
//        var fileUploads: List<FileUploadEntity> = emptyList(),
//        val failedColoniesRows: List<FailedRow> = emptyList(),
//        val totalColoniesRows: Int = 0,
//        val failedObserversRows: List<FailedRow> = emptyList(),
//        val totalObserversRows: Int = 0,
//        val isError: Boolean = false,
//        val errorText: String = "",
//        val date: String,
//        val deviceCoordinates: Coordinates? = null,
//        val location: GeoLocation? = null,
//        val deviceID: String
//    )
//
//    private val _uiState = MutableStateFlow(
//        UiState(
//            deviceID = getDeviceName(context)
//        )
//    )
//
//    val uiState: StateFlow<UiState> = _uiState

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

    // Location following
    var isFollowingLocation = mutableStateOf(false)
    private var lastKnownCoordinates: Coordinates? = null
    private val _currentLocation = MutableStateFlow<GeoLocation?>(null)
    val currentLocation: MutableStateFlow<GeoLocation?> = _currentLocation

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
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

                _currentLocation.value = geoLocation

//                _uiState.value = uiState.value.copy(
//                    location = geoLocation // Update with the latest location
//                )

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

//    private fun getDeviceName(context: Context): String {
//        return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
//            ?: "Unknown Device"
//    }

}