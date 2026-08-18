package weddellseal.markrecap.ui.recentobservations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord

class ObservationsToDisplayTest {

    @Test
    fun momAndPupWithDummyPupTagStayGrouped() {
        val pup = pupRecord(id = 2, tag = "0000D", relative = "1234A")
        val mom = momRecord(id = 1, tag = "1234A", relativeOne = "0000D")

        val grouped = observationsToDisplay(listOf(mom, pup))

        assertEquals(1, grouped.size)
        val row = grouped.single() as DisplayObservation.WithPups
        assertEquals("1234A", row.primarySeal.tagIDOne)
        assertEquals("0000D", row.pupOne?.tagIDOne)
        assertNull(row.pupTwo)
    }

    @Test
    fun momWithDummyTagDoesNotAttachHerselfAsPup() {
        val pup = pupRecord(id = 2, tag = "5678B", relative = "0000D")
        val mom = momRecord(id = 1, tag = "0000D", relativeOne = "5678B")

        val grouped = observationsToDisplay(listOf(mom, pup))

        val row = grouped.single() as DisplayObservation.WithPups
        assertEquals("0000D", row.primarySeal.tagIDOne)
        assertEquals("5678B", row.pupOne?.tagIDOne)
        assertNull(row.pupTwo)
    }

    @Test
    fun noTagRelativeIdIsNotTreatedAsASecondPup() {
        val pup = pupRecord(id = 2, tag = "5678B", relative = "1234A")
        val mom = momRecord(id = 1, tag = "1234A", relativeOne = "5678B", relativeTwo = "NoTag")

        val grouped = observationsToDisplay(listOf(mom, pup))

        val row = grouped.single() as DisplayObservation.WithPups
        assertEquals("5678B", row.pupOne?.tagIDOne)
        assertNull(row.pupTwo)
    }

    @Test
    fun incompleteNoTagPupIsStandaloneNotAttachedAsRelative() {
        val ghost = TestFixtures.minimalObservationRecord().copy(
            id = 3,
            ageClass = SealAgeClass.PUP.alpha,
            sex = "",
            numRelatives = "",
            tagIDOne = "",
            tagIDTwo = "NoTag",
            tagEvent = "",
        )
        val pup = pupRecord(id = 2, tag = "0000D", relative = "1234A")
        val mom = momRecord(id = 1, tag = "1234A", relativeOne = "0000D", relativeTwo = "NoTag")

        val grouped = observationsToDisplay(listOf(ghost, mom, pup))

        assertTrue(grouped.any { it is DisplayObservation.Standalone && it.primarySeal.id == 3 })
        val momRow = grouped.filterIsInstance<DisplayObservation.WithPups>().single()
        assertEquals("0000D", momRow.pupOne?.tagIDOne)
        assertNull(momRow.pupTwo)
    }

    @Test
    fun otherAdultWithDummyTagDoesNotStealPupFromMomPupPair() {
        val otherAdult = TestFixtures.minimalObservationRecord().copy(
            id = 10,
            ageClass = SealAgeClass.ADULT.alpha,
            sex = SealSex.MALE.alpha,
            numRelatives = "0",
            tagIDOne = "0000D",
        )
        val pup = pupRecord(id = 2, tag = "0000D", relative = "1234A")
        val mom = momRecord(id = 1, tag = "1234A", relativeOne = "0000D")

        val grouped = observationsToDisplay(listOf(otherAdult, mom, pup))

        val momRow = grouped.filterIsInstance<DisplayObservation.WithPups>().single()
        assertEquals(2, momRow.pupOne?.id)
        assertEquals("0000D", momRow.pupOne?.tagIDOne)
        assertTrue(grouped.any { it is DisplayObservation.Standalone && it.primarySeal.id == 10 })
    }

    @Test
    fun twoDummyPupsAttachFirstMatchingPupInNewestFirstList() {
        val laterDummyPup = pupRecord(id = 10, tag = "0000D", relative = "9999Z")
        val pairPup = pupRecord(id = 2, tag = "0000D", relative = "1234A")
        val mom = momRecord(id = 1, tag = "1234A", relativeOne = "0000D")

        val grouped = observationsToDisplay(listOf(laterDummyPup, pairPup, mom))

        val momRow = grouped.filterIsInstance<DisplayObservation.WithPups>().single {
            it.primarySeal.id == 1
        }
        assertEquals(10, momRow.pupOne?.id)
    }

    private fun momRecord(
        id: Int,
        tag: String,
        relativeOne: String,
        relativeTwo: String = "",
    ): ObservationRecord = TestFixtures.minimalObservationRecord().copy(
        id = id,
        ageClass = SealAgeClass.ADULT.alpha,
        sex = SealSex.FEMALE.alpha,
        numRelatives = "1",
        tagIDOne = tag,
        relativeTagIDOne = relativeOne,
        relativeTagIDTwo = relativeTwo,
    )

    private fun pupRecord(
        id: Int,
        tag: String,
        relative: String,
    ): ObservationRecord = TestFixtures.minimalObservationRecord().copy(
        id = id,
        ageClass = SealAgeClass.PUP.alpha,
        sex = SealSex.UNKNOWN.alpha,
        numRelatives = "1",
        tagIDOne = tag,
        relativeTagIDOne = relative,
    )
}
