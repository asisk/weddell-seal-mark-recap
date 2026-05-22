package weddellseal.markrecap.ui.tagretag.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.ui.home.ObservationMetadata

class BuildObservationRecordTest {

    @Test
    fun propagatesMetadataColonyObserversAndLocation() {
        val colony = TestFixtures.sampleColony(location = "Big Razor")
        val metadata = ObservationMetadata(
            selectedColony = colony,
            selectedObservers = listOf("AB", "CD"),
            deviceID = "tablet-1",
            currentSeason = "2025 Test",
        )
        val seal = TestFixtures.completePrimaryMarkedSeal()
        val loc = TestFixtures.sampleGeoLocation()

        val record = buildObservationRecord(loc, seal, "", "", "", metadata)

        assertEquals("tablet-1", record.deviceID)
        assertEquals("2025 Test", record.season)
        assertEquals("Big Razor", record.colony)
        assertEquals("AB, CD", record.observerInitials)
        assertEquals(loc.coordinates.latitude.toString(), record.latitude)
        assertEquals(loc.coordinates.longitude.toString(), record.longitude)
    }

    @Test
    fun noTagMapsToMarkedEventAndNoTagColumn() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            isNoTag = true,
            tagEventType = TagEventType.MARKED,
        )
        val record = buildObservationRecord(
            null,
            seal,
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertEquals(TagEventType.MARKED.alpha, record.tagEvent)
        assertEquals("NoTag", record.tagIDOne)
    }

    @Test
    fun newEventAddsPlusIndicatorsForTwoTags() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.NEW,
            tagNumber = "999",
            tagAlpha = "Z",
            numTags = "2",
            condition = SealCondition.GOOD,
        )
        val record = buildObservationRecord(
            null,
            seal,
            "",
            "rel1",
            "rel2",
            TestFixtures.sampleMetadata(),
        )

        assertEquals("+", record.tagOneIndicator)
        assertEquals("+", record.tagTwoIndicator)
        assertEquals("999Z", record.tagIDOne)
        assertEquals("999Z", record.tagIDTwo)
        assertEquals("rel1", record.relativeTagIDOne)
        assertEquals("rel2", record.relativeTagIDTwo)
    }

    @Test
    fun retagWithMissingTagReasonSetsSecondOldTagNoTag() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.RETAG,
            tagNumber = "111",
            tagAlpha = "A",
            numTags = "1",
            oldTagNumber = "222",
            oldTagAlpha = "B",
            reasonForRetag = RetagReason.ONE_OF_FOUR,
            condition = SealCondition.GOOD,
            hasEdits = true,
        )
        val record = buildObservationRecord(
            null,
            seal,
            "edited note",
            "",
            "",
            TestFixtures.sampleMetadata().copy(originalDate = "2024-06-01", originalTimestamp = "08:00:00"),
        )

        assertEquals(TagEventType.RETAG.alpha, record.tagEvent)
        assertEquals("222B", record.oldTagIDOne)
        assertEquals("NoTag", record.oldTagIDTwo)
        assertTrue(record.comments.contains("Edited"))
        assertTrue(record.comments.contains("edited note"))
        assertEquals("2024-06-01", record.date)
        assertEquals("08:00:00", record.time)
    }

    @Test
    fun retagWithSelectedReasonIncludesReasonInComments() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.RETAG,
            tagNumber = "111",
            tagAlpha = "A",
            numTags = "1",
            oldTagNumber = "222",
            oldTagAlpha = "B",
            reasonForRetag = RetagReason.ONE_OF_FOUR,
            condition = SealCondition.GOOD,
        )

        val record = buildObservationRecord(
            null,
            seal,
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertEquals(RetagReason.ONE_OF_FOUR.description, record.retagReason)
        assertTrue(
            record.comments.contains("Reason for Retag: ${RetagReason.ONE_OF_FOUR.description}"),
        )
    }

    @Test
    fun dateTimeUsesCurrentWhenNoEdits() {
        val seal = TestFixtures.completePrimaryNewSeal()
        val record = buildObservationRecord(null, seal, "", "", "", TestFixtures.sampleMetadata())

        assertTrue(record.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(record.time.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }
}
