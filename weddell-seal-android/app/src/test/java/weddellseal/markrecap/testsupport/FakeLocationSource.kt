package weddellseal.markrecap.testsupport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import weddellseal.markrecap.domain.location.LocationSource
import weddellseal.markrecap.domain.location.data.GeoLocation

class FakeLocationSource : LocationSource {
    var startCount = 0
        private set
    var stopCount = 0
        private set

    override suspend fun requestSingleUpdate(): Result<GeoLocation> =
        Result.failure(IllegalStateException("not used in test"))

    override suspend fun locationUpdates(): Flow<GeoLocation> = emptyFlow()

    override fun startLocationUpdates() {
        startCount++
    }

    override fun stopLocationUpdates() {
        stopCount++
    }
}
