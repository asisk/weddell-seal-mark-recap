package weddellseal.markrecap.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.ui.home.ColonyGpsUi

class ColonyMapGeometryTest {

    @Test
    fun otherAndZeroLimitsAreNotDrawable() {
        val other = TestFixtures.sampleColony(location = "Other", n = 0.0, s = 0.0, w = 0.0, e = 0.0)
        val notDetected = TestFixtures.sampleColony(location = ColonyPopulation.NOT_DETECTED)
        assertFalse(other.isDrawableOnMap())
        assertFalse(notDetected.isDrawableOnMap())
    }

    @Test
    fun insideOutsideLocalCategories() {
        assertEquals(
            ColonyMapCategory.Inside,
            TestFixtures.sampleColony(inOut = "Inside").mapCategory(),
        )
        assertEquals(
            ColonyMapCategory.Outside,
            TestFixtures.sampleColony(inOut = "Outside").mapCategory(),
        )
        assertEquals(
            ColonyMapCategory.Local,
            TestFixtures.sampleColony(inOut = "Local", location = "Baxter Meadows").mapCategory(),
        )
    }

    @Test
    fun fitTargetsExcludeBaxterOutsideEnvelope() {
        val hutton = TestFixtures.sampleColony(
            location = "Hutton Cliffs",
            inOut = "Inside",
            n = -77.72,
            s = -77.74,
            w = 166.82,
            e = 166.88,
        )
        val baxter = TestFixtures.sampleColony(
            location = "Baxter Meadows",
            inOut = "Local",
            n = 45.7031,
            s = 45.702,
            w = -111.089,
            e = -111.087,
            adjLat = 45.70255,
            adjLong = -111.088,
        )
        val cape = TestFixtures.sampleColony(
            location = "Cape Washington",
            inOut = "Outside",
            n = -74.633,
            s = -74.683,
            w = 165.167,
            e = 165.5,
        )
        val fit = listOf(hutton, baxter, cape).fitTargetColonies().map { it.location }
        assertTrue(fit.contains("Hutton Cliffs"))
        assertTrue(fit.contains("Cape Washington"))
        assertFalse(fit.contains("Baxter Meadows"))
    }

    @Test
    fun geoJsonMarksActiveColony() {
        val hutton = TestFixtures.sampleColony(
            location = "Hutton Cliffs",
            inOut = "Inside",
            n = -77.72,
            s = -77.74,
            w = 166.82,
            e = 166.88,
        )
        val json = coloniesToGeoJson(listOf(hutton), activeLocationName = "Hutton Cliffs")
        assertTrue(json.contains("\"active\": \"true\""))
        assertTrue(json.contains("Hutton Cliffs"))
        assertTrue(json.contains("Polygon"))
    }
}

class MapStatusLinesTest {

    @Test
    fun dualLineWhenOverrideOn() {
        val live = GeoLocation(
            coordinates = Coordinates(-77.727, 166.85),
            isLiveFix = true,
            accuracyMeters = 5f,
        )
        val detected = TestFixtures.sampleColony(location = "Hutton Cliffs")
        val selected = TestFixtures.sampleColony(location = "Tent Is")
        val lines = mapStatusLines(
            location = live,
            isRefreshingGps = false,
            autoDetectedColony = detected,
            overrideColony = true,
            selectedColony = selected,
        )
        assertTrue(lines.primary.contains("Hutton Cliffs"))
        assertEquals(
            "${MapScreenUi.OVERRIDE_PREFIX}Tent Is (set on Home)",
            lines.secondary,
        )
    }

    @Test
    fun lastKnownDoesNotClaimGpsColony() {
        val cached = GeoLocation(
            coordinates = Coordinates(-77.727, 166.85),
            isLiveFix = false,
        )
        val lines = mapStatusLines(
            location = cached,
            isRefreshingGps = false,
            autoDetectedColony = TestFixtures.sampleColony(location = "Hutton Cliffs"),
            overrideColony = false,
            selectedColony = null,
        )
        assertTrue(lines.primary.contains(ColonyGpsUi.LAST_KNOWN_LABEL))
        assertTrue(lines.primary.contains(ColonyGpsUi.WAITING_FOR_GPS_SHORT))
        assertEquals(null, lines.secondary)
    }

    @Test
    fun activeGpsColonyNameSkipsNotDetected() {
        assertEquals(
            null,
            activeGpsColonyName(
                TestFixtures.sampleColony(location = ColonyPopulation.NOT_DETECTED),
            ),
        )
        assertEquals(
            "Hutton Cliffs",
            activeGpsColonyName(TestFixtures.sampleColony(location = "Hutton Cliffs")),
        )
    }
}
