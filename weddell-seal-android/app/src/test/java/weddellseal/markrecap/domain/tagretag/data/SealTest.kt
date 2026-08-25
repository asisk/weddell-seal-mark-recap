package weddellseal.markrecap.domain.tagretag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import weddellseal.markrecap.ui.utils.getCurrentYear

class SealTest {

    @Test
    fun `seal is incomplete when required fields are missing`() {
        val seal = Seal(sealType = SealType.PRIMARY, sex = SealSex.FEMALE)

        assertFalse(seal.isComplete)
        assertTrue(seal.completenessReasons.any { it.contains("Select an age") })
    }

    @Test
    fun `seal is complete when all required fields are filled`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.TWO,
            tagEventType = TagEventType.MARKED,
            tagNumber = "123",
            tagAlpha = "A",
            numTags = "2",
            condition = SealCondition.GOOD,
        )

        assertTrue(seal.isComplete)
        assertEquals(emptyList<String>(), seal.completenessReasons)
    }

    @Test
    fun `seal validation returns error for invalid tag number length`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.TWO,
            tagEventType = TagEventType.MARKED,
            tagNumber = "12",  // Too short
            tagAlpha = "A",
            numTags = "2",
            condition = SealCondition.GOOD,
        )

        val errors = seal.completenessReasons
        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("Tag number must be 3 or 4 digits") })
    }

    @Test
    fun `seal with isNoTag skips validation`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            isNoTag = true,
            tagEventType = TagEventType.MARKED,
        )

        assertTrue(seal.validationErrors.isEmpty())
    }

    @Test
    fun `pup requires condition when entry started`() {
        val seal = Seal(
            sealType = SealType.PUPONE,
            ageClass = SealAgeClass.PUP,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ONE,
            tagEventType = TagEventType.MARKED,
            tagNumber = "100",
            tagAlpha = "A",
            numTags = "1",
            condition = SealCondition.UNKNOWN,
        )

        assertFalse(seal.isComplete)
        assertTrue(seal.completenessReasons.any { it.contains("condition") })
    }

    @Test
    fun `retag requires reason when entry started`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.RETAG,
            tagNumber = "100",
            tagAlpha = "A",
            numTags = "1",
            oldTagNumber = "99",
            oldTagAlpha = "B",
            reasonForRetag = RetagReason.UNKNOWN,
            condition = SealCondition.GOOD,
        )

        assertFalse(seal.isComplete)
        assertTrue(seal.completenessReasons.any { it.contains("reason for retag") })
    }

    @Test
    fun `adult does not require numRelatives message when relatives unknown before entry`() {
        val seal = Seal(sealType = SealType.PRIMARY)
        assertTrue(seal.completenessReasons.isEmpty())
    }

    /**
     * Parker 2025 season recap: dummy 0000D skips WedCheck / tag-number error checking so
     * technicians are not prompted every time they enter a placeholder for an untagged animal.
     */
    @Test
    fun `marked dummy tag 0000D with no WedCheck match is not flagged`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.MARKED,
            tagNumber = "0000",
            tagAlpha = "D",
            numTags = "1",
            condition = SealCondition.GOOD,
            wedCheckMatch = null,
        )

        assertTrue(seal.isComplete)
        assertTrue(seal.isDummyTag)
        assertEquals(emptyList<String>(), seal.validationErrors)
        assertTrue(seal.isValid)
    }

    @Test
    fun `retag dummy old tag 0000D with no WedCheck match is not flagged`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.RETAG,
            tagNumber = "123",
            tagAlpha = "A",
            oldTagNumber = "0000",
            oldTagAlpha = "D",
            numTags = "1",
            reasonForRetag = RetagReason.OTHER,
            condition = SealCondition.GOOD,
            wedCheckMatch = null,
        )

        assertTrue(seal.isComplete)
        assertTrue(seal.isDummyTag)
        assertEquals(emptyList<String>(), seal.validationErrors)
        assertTrue(seal.isValid)
    }

    @Test
    fun `new dummy tag 0000D skips tag already used even when WedCheck match exists`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.NEW,
            tagNumber = "0000",
            tagAlpha = "D",
            numTags = "1",
            condition = SealCondition.GOOD,
            wedCheckMatch = WedCheckSeal(
                speNo = 1,
                tagIdOne = "0000D",
                sex = SealSex.FEMALE,
                ageClass = SealAgeClass.ADULT,
                numTags = "1",
                condition = SealCondition.GOOD,
                lastSeenSeason = 2026,
            ),
        )

        assertTrue(seal.isDummyTag)
        assertEquals(emptyList<String>(), seal.validationErrors)
    }

    @Test
    fun `marked non-dummy tag with no WedCheck match is still flagged`() {
        val seal = Seal(
            sealType = SealType.PRIMARY,
            ageClass = SealAgeClass.ADULT,
            sex = SealSex.FEMALE,
            numRelatives = SealRelatives.ZERO,
            tagEventType = TagEventType.MARKED,
            tagNumber = "1234",
            tagAlpha = "A",
            numTags = "1",
            condition = SealCondition.GOOD,
            wedCheckMatch = null,
        )

        assertFalse(seal.isDummyTag)
        assertTrue(seal.validationErrors.any { it.contains("Seal not in database") })
    }

    @Test
    fun `marked adult sex mismatch against WedCheck is flagged with description`() {
        // Parker 2025 season recap: one known miss (female adult entered as male with no
        // confirmation). Marked + WedCheck Female + entered Male must flag sex mismatch.
        val seal = markedAdultWithWedCheck(
            enteredSex = SealSex.MALE,
            wedCheckSex = SealSex.FEMALE,
        )

        assertTrue(
            seal.validationErrors.any {
                it.contains("Sex doesn't match") && it.contains("Female")
            },
        )
    }

    @Test
    fun `marked adult matching WedCheck sex is not flagged for sex`() {
        val seal = markedAdultWithWedCheck(
            enteredSex = SealSex.MALE,
            wedCheckSex = SealSex.MALE,
        )

        assertTrue(seal.validationErrors.none { it.contains("Sex doesn't match") })
    }

    @Test
    fun `marked adult does not flag sex when WedCheck sex is missing`() {
        // Parker 2025 season recap: false "are you sure this is a male" when WedCheck sex was blank.
        val seal = markedAdultWithWedCheck(
            enteredSex = SealSex.MALE,
            wedCheckSex = SealSex.NONE,
        )

        assertTrue(seal.validationErrors.none { it.contains("Sex doesn't match") })
    }

    private fun markedAdultWithWedCheck(
        enteredSex: SealSex,
        wedCheckSex: SealSex,
    ) = Seal(
        sealType = SealType.PRIMARY,
        ageClass = SealAgeClass.ADULT,
        sex = enteredSex,
        numRelatives = SealRelatives.ZERO,
        tagEventType = TagEventType.MARKED,
        tagNumber = "1234",
        tagAlpha = "A",
        numTags = "1",
        condition = SealCondition.GOOD,
        wedCheckMatch = WedCheckSeal(
            speNo = 10,
            tagIdOne = "1234A",
            sex = wedCheckSex,
            ageClass = SealAgeClass.ADULT,
            numTags = "1",
            condition = SealCondition.GOOD,
            lastSeenSeason = getCurrentYear(),
        ),
    )
}

class SealConditionTest {

    @Test
    fun `fromCode returns correct enum value`() {
        assertEquals(SealCondition.DEAD, SealCondition.fromCode("0"))
        assertEquals(SealCondition.POOR, SealCondition.fromCode("1"))
        assertEquals(SealCondition.FAIR, SealCondition.fromCode("2"))
        assertEquals(SealCondition.GOOD, SealCondition.fromCode("3"))
        assertEquals(SealCondition.NEWBORN, SealCondition.fromCode("4"))
        assertEquals(SealCondition.NONE, SealCondition.fromCode(null))
        assertEquals(SealCondition.NONE, SealCondition.fromCode("999"))
    }

    @Test
    fun `toCode returns correct code string`() {
        assertEquals("0", SealCondition.DEAD.code)
        assertEquals("1", SealCondition.POOR.code)
        assertEquals("2", SealCondition.FAIR.code)
        assertEquals("3", SealCondition.GOOD.code)
        assertEquals("4", SealCondition.NEWBORN.code)
        assertEquals("", SealCondition.NONE.code)
    }

    @Test
    fun `toLabel returns expected formatted string`() {
        assertEquals("Dead - 0", SealCondition.DEAD.toLabel())
        assertEquals("Poor - 1", SealCondition.POOR.toLabel())
        assertEquals("Fair - 2", SealCondition.FAIR.toLabel())
        assertEquals("Good - 3", SealCondition.GOOD.toLabel())
        assertEquals("Newborn - 4", SealCondition.NEWBORN.toLabel())
        assertEquals("None", SealCondition.NONE.toLabel())
    }

    @Test
    fun `fromLabel parses label string correctly`() {
        assertEquals(SealCondition.DEAD, SealCondition.fromLabel("Dead - 0"))
        assertEquals(SealCondition.POOR, SealCondition.fromLabel("Poor - 1"))
        assertEquals(SealCondition.FAIR, SealCondition.fromLabel("Fair - 2"))
        assertEquals(SealCondition.GOOD, SealCondition.fromLabel("Good - 3"))
        assertEquals(SealCondition.NEWBORN, SealCondition.fromLabel("Newborn - 4"))
        assertEquals(SealCondition.NONE, SealCondition.fromLabel("None"))
        assertEquals(SealCondition.NONE, SealCondition.fromLabel("Invalid - 9"))
    }

    @Test
    fun `comment-only change is an edit but is omitted from the was-now trail`() {
        val original = Seal(
            sealType = SealType.PRIMARY,
            comment = "scar",
            condition = SealCondition.GOOD,
        )
        val updated = original.copy(comment = "scar on left")

        assertTrue(updated.hasChangesFrom(original))
        assertTrue(updated.edits(original).isEmpty())
    }

    @Test
    fun `field edits are recorded even when the comment also changes`() {
        val original = Seal(
            sealType = SealType.PRIMARY,
            comment = "scar",
            condition = SealCondition.GOOD,
        )
        val updated = original.copy(comment = "scar on left", condition = SealCondition.FAIR)

        assertTrue(updated.hasChangesFrom(original))
        val edits = updated.edits(original)
        assertTrue(edits.any { it.contains("condition") })
        assertTrue(edits.none { it.startsWith("comment") })
    }
}