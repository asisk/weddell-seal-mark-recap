package weddellseal.markrecap.testsupport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.GeoLocation

class FakeLocationSource : LocationSource {
    var startCount = 0
        private set
    var stopCount = 0
        private set

    var lastKnown: GeoLocation? = null

    private val updates = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 16)

    override suspend fun requestSingleUpdate(): Result<GeoLocation> =
        Result.failure(IllegalStateException("not used in test"))

    override suspend fun lastKnownLocation(): GeoLocation? = lastKnown

    override suspend fun locationUpdates(): Flow<GeoLocation> = updates

    override fun startLocationUpdates() {
        startCount++
    }

    override fun stopLocationUpdates() {
        stopCount++
    }

    fun emit(location: GeoLocation) {
        updates.tryEmit(location)
    }
}
