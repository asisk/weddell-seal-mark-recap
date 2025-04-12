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
     * Start location updates.
     */
    suspend fun startLocationUpdates()

    /**
     * Stop location updates.
     */
    suspend fun stopLocationUpdates()
}

