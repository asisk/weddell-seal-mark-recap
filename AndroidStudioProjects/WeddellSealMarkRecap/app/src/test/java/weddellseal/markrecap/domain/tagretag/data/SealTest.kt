package weddellseal.markrecap.domain.tagretag.data

import org.junit.Assert.*
import org.junit.Test

class SealTest {

    @Test
    fun `seal is incomplete when required fields are missing`() {
        val seal = Seal(name = "TestSeal")

        assertFalse(seal.isComplete)
        assertTrue(seal.completenessReasons.any { it.contains("Select an age") })
    }

    @Test
    fun `seal is complete when all required fields are filled`() {
        val seal = Seal(
            name = "TestSeal",
            age = "Adult",
            sex = "Female",
            numRelatives = "2",
            tagEventType = "Marked",
            tagNumber = "123",
            numTags = "2"
        )

        assertTrue(seal.isComplete)
        assertEquals(emptyList<String>(), seal.completenessReasons)
    }

    @Test
    fun `seal validation returns error for invalid tag number length`() {
        val seal = Seal(
            name = "TestSeal",
            age = "Adult",
            sex = "Female",
            numRelatives = "2",
            tagEventType = "Marked",
            tagNumber = "12",  // Too short
            numTags = "2"
        )

        val errors = seal.validationErrors
        assertFalse(errors.isEmpty())
        assertFalse(errors.any { it.contains("Tag number must be 3 or 4 digits") })
    }

    @Test
    fun `seal with isNoTag skips validation`() {
        val seal = Seal(
            name = "Tagless",
            isNoTag = true,
            tagEventType = "Marked"
        )

        assertTrue(seal.validationErrors.isEmpty())
    }

    @Test
    fun `isStarted flag tracks interaction`() {
        val untouched = Seal(name = "QuietSeal")
        val edited = untouched.copy(age = "Pup", isStarted = true)

        assertFalse(untouched.isStarted)
        assertTrue(edited.isStarted)
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
        assertEquals("None - ", SealCondition.NONE.toLabel())
    }

    @Test
    fun `fromLabel parses label string correctly`() {
        assertEquals(SealCondition.DEAD, SealCondition.fromLabel("Dead - 0"))
        assertEquals(SealCondition.POOR, SealCondition.fromLabel("Poor - 1"))
        assertEquals(SealCondition.FAIR, SealCondition.fromLabel("Fair - 2"))
        assertEquals(SealCondition.GOOD, SealCondition.fromLabel("Good - 3"))
        assertEquals(SealCondition.NEWBORN, SealCondition.fromLabel("Newborn - 4"))
        assertEquals(SealCondition.NONE, SealCondition.fromLabel("None - "))
        assertEquals(SealCondition.NONE, SealCondition.fromLabel("Invalid - 9"))
    }
}