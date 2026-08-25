package weddellseal.markrecap.domain.location

import kotlinx.coroutines.flow.Flow
import weddellseal.markrecap.domain.location.data.GeoLocation

/**
 * A source of device location information.
 */
interface LocationSource {
    /**
     * Request a single location update.
     */
    suspend fun requestSingleUpdate(): Result<GeoLocation>

    /**
     * Get a flow of location updates.
     */
    suspend fun locationUpdates(): Flow<GeoLocation>

    /**
     * Start location updates. Not suspending so it can run during ViewModel teardown.
     */
    fun startLocationUpdates()

    /**
     * Stop location updates and release listeners and worker threads.
     */
    fun stopLocationUpdates()
}

