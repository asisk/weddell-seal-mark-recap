package weddellseal.markrecap.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagProcessorTest {

    @Test
    fun testProcessTags_withValidTags() {
        val result = processTags("123A", "123A")
        assertEquals(2, result.numTags)
        assertEquals("123A", result.tagId)
        assertEquals("A", result.tagAlpha)
        assertEquals(123, result.tagNumber)
    }

    @Test
    fun testProcessTags_withInvalidTag1() {
        val result = processTags("NA", "789C")
        assertEquals(1, result.numTags)
        assertEquals("789C", result.tagId)
        assertEquals("C", result.tagAlpha)
        assertEquals(789, result.tagNumber)
    }

    @Test
    fun testProcessTags_withInvalidTag2() {
        val result = processTags("101D", "NoTag")
        assertEquals(1, result.numTags)
        assertEquals("101D", result.tagId)
        assertEquals("D", result.tagAlpha)
        assertEquals(101, result.tagNumber)
    }

    @Test
    fun testProcessTags_withInvalidTag_badValue_alphas() {
        val result = processTags("badValue", "101D")
        assertEquals(1, result.numTags)
        assertEquals("101D", result.tagId)
        assertEquals("D", result.tagAlpha)
        assertEquals(101, result.tagNumber)
    }

    @Test
    fun testProcessTags_withInvalidTag_badValue_numbers() {
        val result = processTags("1234", "101D")
        assertEquals(1, result.numTags)
        assertEquals("101D", result.tagId)
        assertEquals("D", result.tagAlpha)
        assertEquals(101, result.tagNumber)
    }

    @Test
    fun testProcessTags_withBothInvalidTags() {
        val result = processTags("NoTag", "NA")
        assertEquals(0, result.numTags)
        assertEquals("", result.tagId)
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)
    }

    @Test
    fun testProcessTags_withEmptyTags() {
        val result = processTags("", "")
        assertEquals(0, result.numTags)
        assertEquals("", result.tagId)
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)
    }

    @Test
    fun testProcessTags_withNullTags() {
        val result = processTags(null, null)
        assertEquals(0, result.numTags)
        assertEquals("", result.tagId)
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)
    }
}