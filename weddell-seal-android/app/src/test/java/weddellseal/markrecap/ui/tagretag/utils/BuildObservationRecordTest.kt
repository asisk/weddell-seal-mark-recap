package weddellseal.markrecap.ui.tagretag.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun retagWithNoneOrUnknownReasonOmitsReasonFromComments() {
        listOf(RetagReason.NONE, RetagReason.UNKNOWN).forEach { reason ->
            val record = buildObservationRecord(
                null,
                retagSeal(reasonForRetag = reason),
                "",
                "",
                "",
                TestFixtures.sampleMetadata(),
            )

            assertEquals(reason.description, record.retagReason)
            assertFalse(record.comments.contains("Reason for Retag:"))
        }
    }

    @Test
    fun newEventWithOldTagMarksIncludesComment() {
        val record = buildObservationRecord(
            null,
            TestFixtures.completePrimaryNewSeal().copy(oldTagMarks = true),
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertTrue(record.comments.contains("old tag marks; "))
    }

    @Test
    fun retagEventWithOldTagMarksIncludesComment() {
        val record = buildObservationRecord(
            null,
            retagSeal().copy(oldTagMarks = true),
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertTrue(record.comments.contains("old tag marks; "))
    }

    @Test
    fun markedEventOmitsOldTagMarksFromCommentsEvenWhenFlagSet() {
        val record = buildObservationRecord(
            null,
            TestFixtures.completePrimaryMarkedSeal().copy(oldTagMarks = true),
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertFalse(record.comments.contains("old tag marks"))
    }

    @Test
    fun nonRetagEventOmitsReasonFromCommentsEvenWhenReasonSet() {
        val seal = retagSeal(reasonForRetag = RetagReason.OTHER)
            .copy(tagEventType = TagEventType.NEW)

        val record = buildObservationRecord(
            null,
            seal,
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertFalse(record.comments.contains("Reason for Retag:"))
    }

    @Test
    fun retagReasonAppearsBeforeUserCommentInCommentString() {
        val record = buildObservationRecord(
            null,
            retagSeal(reasonForRetag = RetagReason.OTHER).copy(
                pupPeed = true,
                comment = "field note",
            ),
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        val reasonSnippet = "Reason for Retag: Other;"
        assertTrue(record.comments.startsWith("pup peed; "))
        assertTrue(record.comments.contains(reasonSnippet))
        assertTrue(record.comments.endsWith("field note"))
        assertTrue(record.comments.indexOf(reasonSnippet) < record.comments.indexOf("field note"))
    }

    @Test
    fun dateTimeUsesCurrentWhenNoEdits() {
        val seal = TestFixtures.completePrimaryNewSeal()
        val record = buildObservationRecord(null, seal, "", "", "", TestFixtures.sampleMetadata())

        assertTrue(record.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(record.time.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    /** Fix #5: new observations use id = 0 so Room auto-generates the primary key. */
    @Test
    fun newObservationUsesAutoGeneratedId() {
        val record = buildObservationRecord(
            null,
            TestFixtures.completePrimaryNewSeal(),
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        assertEquals(0, record.id)
        assertNull(record.updatedAt)
    }

    @Test
    fun flaggedForReviewPutsConfirmationInEdtAndComments() {
        // Parker 2025 season recap: "technician confirmed" showed on screen but was missing
        // from export comments (only flaggedEntry / edt). Proofing looks at comments.
        val technicianNote = "photo taken, tags look worn"
        val seal = TestFixtures.completePrimaryMarkedSeal().copy(
            flaggedForReview = true,
            comment = technicianNote,
        )
        // No WedCheck match → validation mismatch on Marked seals.

        val record = buildObservationRecord(
            null,
            seal,
            "",
            "",
            "",
            TestFixtures.sampleMetadata(),
        )

        // Validation messages stay in comments with the technician note; confirmation is
        // also copied into comments so field proofing can see it without the edt column.
        assertTrue(record.comments.contains(technicianNote))
        assertTrue(record.comments.contains("Seal not in database"))
        assertTrue(record.comments.contains("technician confirmed"))
        assertEquals("technician confirmed", record.flaggedEntry)
    }

    /** Fix #5: edits are append-only and should create a new row. */
    @Test
    fun editedObservationCreatesNewRow() {
        val seal = TestFixtures.completePrimaryMarkedSeal().copy(
            hasEdits = true,
            observationID = 42,
        )
        val record = buildObservationRecord(
            null,
            seal,
            "condition changed",
            "",
            "",
            TestFixtures.sampleMetadata().copy(
                originalDate = "2024-06-01",
                originalTimestamp = "08:00:00",
            ),
        )

        assertEquals(0, record.id)
        assertNull(record.updatedAt)
        assertTrue(record.comments.contains("Edited"))
    }

    private fun retagSeal(
        reasonForRetag: RetagReason = RetagReason.ONE_OF_FOUR,
    ) = Seal(
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
        reasonForRetag = reasonForRetag,
        condition = SealCondition.GOOD,
    )
}
