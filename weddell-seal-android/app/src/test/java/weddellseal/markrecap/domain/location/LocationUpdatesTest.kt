package weddellseal.markrecap.domain.location

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation

@OptIn(ExperimentalCoroutinesApi::class)
class LocationUpdatesTest {

    private val cached = GeoLocation(
        coordinates = Coordinates(-77.5, 166.5),
        isLiveFix = false,
    )
    private val liveA = GeoLocation(
        coordinates = Coordinates(-77.5, 166.5),
        isLiveFix = true,
    )
    private val liveB = GeoLocation(
        coordinates = Coordinates(-77.51, 166.51),
        isLiveFix = true,
    )
    private val liveC = GeoLocation(
        coordinates = Coordinates(-77.52, 166.52),
        isLiveFix = true,
    )

    @Test
    fun cachedAndLiveAtSameCoordinates_areNotEquivalent() {
        assertFalse(areLocationsEquivalentForUi(cached, liveA))
    }

    @Test
    fun twoLiveFixesAtSameCoordinates_areEquivalent() {
        assertTrue(areLocationsEquivalentForUi(liveA, liveA.copy(accuracyMeters = 8f)))
    }

    @Test
    fun accuracyCrossingColonyThreshold_isNotEquivalent() {
        val poor = liveA.copy(accuracyMeters = 180f)
        val good = liveA.copy(accuracyMeters = 15f)
        assertFalse(areLocationsEquivalentForUi(poor, good))
    }

    @Test
    fun shouldEmit_firstLiveImmediately_evenIfSampleWindowHasNotElapsed() {
        assertTrue(
            shouldEmitSampledLocation(
                isLiveFix = true,
                hasEmittedLiveFix = false,
                elapsedSinceLastEmitMs = 0L,
            )
        )
    }

    @Test
    fun shouldEmit_cachedAlways() {
        assertTrue(
            shouldEmitSampledLocation(
                isLiveFix = false,
                hasEmittedLiveFix = true,
                elapsedSinceLastEmitMs = 10L,
            )
        )
    }

    @Test
    fun shouldEmit_laterLiveRespectsSamplePeriod() {
        assertFalse(
            shouldEmitSampledLocation(
                isLiveFix = true,
                hasEmittedLiveFix = true,
                elapsedSinceLastEmitMs = 500L,
            )
        )
        assertTrue(
            shouldEmitSampledLocation(
                isLiveFix = true,
                hasEmittedLiveFix = true,
                elapsedSinceLastEmitMs = 2000L,
            )
        )
    }

    @Test
    fun shouldUseIncomingLocation_ignoresCachedOnceLive() {
        assertFalse(shouldUseIncomingLocation(currentIsLive = true, incomingIsLive = false))
        assertTrue(shouldUseIncomingLocation(currentIsLive = true, incomingIsLive = true))
        assertTrue(shouldUseIncomingLocation(currentIsLive = false, incomingIsLive = false))
        assertTrue(shouldUseIncomingLocation(currentIsLive = false, incomingIsLive = true))
    }

    @Test
    fun isAccurateEnoughForColonyMiss_poorFixWaits() {
        assertFalse(isAccurateEnoughForColonyMiss(180f))
        assertTrue(isAccurateEnoughForColonyMiss(15f))
        assertTrue(isAccurateEnoughForColonyMiss(null))
        assertTrue(isAccurateEnoughForColonyMiss(50f))
    }

    @Test
    fun sampleAfterFirstLiveFix_emitsFirstLiveWithoutWaiting() = runTest {
        val nowMs = { testScheduler.currentTime }
        val source = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 8)
        val emitted = mutableListOf<GeoLocation>()
        backgroundScope.launch {
            source.sampleAfterFirstLiveFix(samplePeriodMs = 2000L, nowMs = nowMs)
                .collect { emitted += it }
        }
        runCurrent()

        source.emit(cached)
        source.emit(liveA)
        runCurrent()
        assertEquals(listOf(cached, liveA), emitted)

        // After the sample window with nothing buffered, the next live fix emits immediately.
        advanceTimeBy(2000)
        source.emit(liveC)
        runCurrent()
        assertEquals(listOf(cached, liveA, liveC), emitted)
    }

    @Test
    fun sampleAfterFirstLiveFix_emitsLatestBufferedFixWhenWindowExpires() = runTest {
        val nowMs = { testScheduler.currentTime }
        val source = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 8)
        val emitted = mutableListOf<GeoLocation>()
        backgroundScope.launch {
            source.sampleAfterFirstLiveFix(samplePeriodMs = 2000L, nowMs = nowMs)
                .collect { emitted += it }
        }
        runCurrent()

        source.emit(liveA)
        runCurrent()
        assertEquals(listOf(liveA), emitted)

        advanceTimeBy(500)
        source.emit(liveB)
        runCurrent()
        assertEquals(listOf(liveA), emitted)

        // No further provider callbacks — the buffered fix must still flush.
        advanceTimeBy(1500)
        runCurrent()
        assertEquals(listOf(liveA, liveB), emitted)
    }

    @Test
    fun sampleAfterFirstLiveFix_keepsOnlyLatestFixInsideWindow() = runTest {
        val nowMs = { testScheduler.currentTime }
        val source = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 8)
        val emitted = mutableListOf<GeoLocation>()
        backgroundScope.launch {
            source.sampleAfterFirstLiveFix(samplePeriodMs = 2000L, nowMs = nowMs)
                .collect { emitted += it }
        }
        runCurrent()

        source.emit(liveA)
        runCurrent()

        advanceTimeBy(200)
        source.emit(liveB)
        runCurrent()
        advanceTimeBy(200)
        source.emit(liveC)
        runCurrent()
        assertEquals(listOf(liveA), emitted)

        // Window opened at t=0; flush is due at t=2000 regardless of later overwrites.
        advanceTimeBy(1600)
        runCurrent()
        assertEquals(listOf(liveA, liveC), emitted)
    }

    @Test
    fun locationRequestSettings_acquireIsEager_trackIsCalmer() {
        val acquire = locationRequestSettings(LocationUpdatePhase.ACQUIRE)
        assertEquals(LOCATION_ACQUIRE_INTERVAL_MS, acquire.intervalMs)
        assertEquals(LOCATION_ACQUIRE_MIN_INTERVAL_MS, acquire.minUpdateIntervalMs)
        assertEquals(null, acquire.maxUpdateDelayMs)
        assertEquals(LOCATION_MIN_UPDATE_DISTANCE_METERS, acquire.minUpdateDistanceMeters)

        val track = locationRequestSettings(LocationUpdatePhase.TRACK)
        assertEquals(LOCATION_TRACK_INTERVAL_MS, track.intervalMs)
        assertEquals(LOCATION_TRACK_MIN_INTERVAL_MS, track.minUpdateIntervalMs)
        assertEquals(LOCATION_TRACK_MAX_UPDATE_MS, track.maxUpdateDelayMs)
        assertEquals(LOCATION_MIN_UPDATE_DISTANCE_METERS, track.minUpdateDistanceMeters)

        assertTrue(track.intervalMs > acquire.intervalMs)
    }
}
