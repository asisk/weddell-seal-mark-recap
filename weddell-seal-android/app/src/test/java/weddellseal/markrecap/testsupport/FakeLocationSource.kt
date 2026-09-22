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
    var singleUpdateCount = 0
        private set

    var lastKnown: GeoLocation? = null

    /** Next result returned from [requestSingleUpdate]; also emitted on the updates flow. */
    var nextSingleUpdate: Result<GeoLocation> =
        Result.failure(IllegalStateException("no single update configured"))

    /** Optional gate so tests can observe mid-refresh state before the fix returns. */
    var singleUpdateGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    // Replay the latest fix so a collector that starts after emit still receives it.
    // Instrumented tests emit on the main thread in the same turn as onPermissionsResult,
    // which can run before viewModelScope starts collecting.
    private val updates = MutableSharedFlow<GeoLocation>(replay = 1, extraBufferCapacity = 16)

    override suspend fun requestSingleUpdate(): Result<GeoLocation> {
        singleUpdateCount++
        singleUpdateGate?.await()
        val result = nextSingleUpdate
        result.getOrNull()?.let { updates.tryEmit(it) }
        return result
    }

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
