package weddellseal.markrecap.domain.location

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation

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
    fun sampleAfterFirstLiveFix_emitsFirstLiveWithoutWaiting() = runBlocking {
        var now = 0L
        val emitted = flow {
            emit(cached)
            emit(liveA)
            now = 500L
            emit(liveB)
            now = 2500L
            emit(liveC)
        }.sampleAfterFirstLiveFix(samplePeriodMs = 2000L) { now }.toList()

        assertEquals(listOf(cached, liveA, liveC), emitted)
    }
}
