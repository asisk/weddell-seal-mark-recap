package weddellseal.markrecap.data.location

import kotlinx.coroutines.flow.Flow

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

