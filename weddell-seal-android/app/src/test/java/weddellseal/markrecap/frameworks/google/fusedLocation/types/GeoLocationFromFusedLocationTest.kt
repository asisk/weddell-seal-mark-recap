package weddellseal.markrecap.frameworks.google.fusedLocation.types

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.domain.location.data.GeoLocation

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeoLocationFromFusedLocationTest {

    @Test
    fun fromFusedLocation_keepsProviderTimestampAccuracyAndLiveFlag() {
        val fused = Location("gps").apply {
            latitude = -77.6301
            longitude = 166.4122
            accuracy = 12.5f
            time = 1_000L
            altitude = 10.0
            bearing = 90f
        }

        val cached = GeoLocation.fromFusedLocation(fused, isLiveFix = false)
        assertFalse(cached.isLiveFix)
        assertEquals(12.5f, cached.accuracyMeters)
        assertEquals(-77.6301, cached.coordinates.latitude, 0.000_001)
        assertTrue(
            "Expected provider epoch, was ${cached.updatedDate}",
            cached.updatedDate!!.contains("1970") || cached.updatedDate.contains("1969"),
        )

        val live = GeoLocation.fromFusedLocation(fused, isLiveFix = true)
        assertTrue(live.isLiveFix)
        assertEquals(12.5f, live.accuracyMeters)
    }
}
