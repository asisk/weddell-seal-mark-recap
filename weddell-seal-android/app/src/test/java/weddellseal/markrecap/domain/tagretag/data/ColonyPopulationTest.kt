package weddellseal.markrecap.domain.tagretag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parker 2025 season recap: White Island population display and Erebus Bay vs White Island highlight. */
class ColonyPopulationTest {

    @Test
    fun whiteIslandLocationMapsToWhiteIslandPopulation() {
        assertEquals(
            ColonyPopulation.WHITE_ISLAND,
            ColonyPopulation.forLocation("White Island"),
        )
    }

    @Test
    fun erebusBayColoniesMapToErebusBayPopulation() {
        assertEquals(ColonyPopulation.EREBUS_BAY, ColonyPopulation.forLocation("Turtle Rock"))
        assertEquals(ColonyPopulation.EREBUS_BAY, ColonyPopulation.forLocation("Hutton Cliffs"))
        assertEquals(ColonyPopulation.EREBUS_BAY, ColonyPopulation.forLocation("Erebus Bay"))
    }

    @Test
    fun unknownGpsDoesNotMapToAPopulation() {
        assertNull(ColonyPopulation.forLocation(null))
        assertNull(ColonyPopulation.forLocation(""))
        assertNull(ColonyPopulation.forLocation(ColonyPopulation.NOT_DETECTED))
    }

    @Test
    fun matchingPopulationsAreNotHighlighted() {
        assertFalse(
            ColonyPopulation.shouldHighlightMismatch("White Island", "White Island"),
        )
        assertFalse(
            ColonyPopulation.shouldHighlightMismatch("Erebus Bay", "Turtle Rock"),
        )
    }

    @Test
    fun mismatchedPopulationsAreHighlighted() {
        assertTrue(
            ColonyPopulation.shouldHighlightMismatch("White Island", "Turtle Rock"),
        )
        assertTrue(
            ColonyPopulation.shouldHighlightMismatch("Erebus Bay", "White Island"),
        )
    }

    @Test
    fun unknownGpsDoesNotHighlight() {
        assertFalse(
            ColonyPopulation.shouldHighlightMismatch("White Island", null),
        )
        assertFalse(
            ColonyPopulation.shouldHighlightMismatch(
                "White Island",
                ColonyPopulation.NOT_DETECTED,
            ),
        )
    }

    @Test
    fun whiteIslandEnterPhotoPrompt_whenGpsIsNotWhiteIsland() {
        assertTrue(
            ColonyPopulation.shouldPromptWhiteIslandPhotoOnEnter("White Island", "Turtle Rock"),
        )
        assertTrue(
            ColonyPopulation.shouldPromptWhiteIslandPhotoOnEnter("White Island", null),
        )
        assertTrue(
            ColonyPopulation.shouldPromptWhiteIslandPhotoOnEnter(
                "White Island",
                ColonyPopulation.NOT_DETECTED,
            ),
        )
        assertFalse(
            ColonyPopulation.shouldPromptWhiteIslandPhotoOnEnter("White Island", "White Island"),
        )
        assertFalse(
            ColonyPopulation.shouldPromptWhiteIslandPhotoOnEnter("Erebus Bay", "White Island"),
        )
    }
}
