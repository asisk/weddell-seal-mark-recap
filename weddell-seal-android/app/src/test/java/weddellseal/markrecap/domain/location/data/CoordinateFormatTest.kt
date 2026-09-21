package weddellseal.markrecap.domain.location.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateFormatTest {

    @Test
    fun padsToFiveDecimalPlaces() {
        assertEquals("-77.50000", (-77.5).toCoordinateString())
        assertEquals("166.79410", 166.7941.toCoordinateString())
    }

    @Test
    fun roundsTheSixthDecimalPlace() {
        assertEquals("-77.73004", (-77.730041).toCoordinateString())
        assertEquals("-77.73005", (-77.730046).toCoordinateString())
        assertEquals("166.79417", 166.794166.toCoordinateString())
    }

    @Test
    fun displayStringKeepsTheExistingGap() {
        val coordinates = Coordinates(latitude = -77.5, longitude = 166.5)
        assertEquals("-77.50000    166.50000", coordinates.toDisplayString())
        assertEquals(
            "-77.50000    166.50000",
            GeoLocation(coordinates).toLocationString(),
        )
    }

    @Test
    fun storedStringsFormatWhenNumericAndStayWhenNot() {
        assertEquals("-77.12340", "-77.1234".toCoordinateDisplayString())
        assertEquals("166.77000", "166.77".toCoordinateDisplayString())
        assertEquals("", "".toCoordinateDisplayString())
        assertEquals("null", "null".toCoordinateDisplayString())
    }
}
