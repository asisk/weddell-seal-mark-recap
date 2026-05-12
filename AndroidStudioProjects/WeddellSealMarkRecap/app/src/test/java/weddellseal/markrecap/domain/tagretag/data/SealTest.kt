package weddellseal.markrecap.domain.tagretag.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}