package weddellseal.markrecap.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagProcessorTest {

    @Test
    fun testProcessTags_withValidTags() {
        var numTags = 0
        val result = processTags("123A")
        if (result.tagValid) {
            numTags++
        }
        assertEquals("A", result.tagAlpha)
        assertEquals(123, result.tagNumber)

        val resultTwo = processTags("123A")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("A", resultTwo.tagAlpha)
        assertEquals(123, resultTwo.tagNumber)

        assertEquals(2, numTags)
    }

    @Test
    fun testProcessTags_withInvalidTag1() {
        var numTags = 0
        val result = processTags("NA")
        if (result.tagValid) {
            numTags++
        }

        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)

        val resultTwo = processTags("789C")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("C", resultTwo.tagAlpha)
        assertEquals(789, resultTwo.tagNumber)

        assertEquals(1, numTags)
    }

    @Test
    fun testProcessTags_withInvalidTag2() {
        var numTags = 0
        val result = processTags("101D")
        if (result.tagValid) {
            numTags++
        }
        assertEquals("D", result.tagAlpha)
        assertEquals(101, result.tagNumber)

        val resultTwo = processTags( "NoTag")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("", resultTwo.tagAlpha)
        assertEquals(0, resultTwo.tagNumber)

        assertEquals(1, numTags)
    }

    @Test
    fun testProcessTags_withInvalidTag_badValue_alphas() {
        var numTags = 0
        val result = processTags("badValue")
        if (result.tagValid) {
            numTags++
        }
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)

        val resultTwo = processTags("101D")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("D", resultTwo.tagAlpha)
        assertEquals(101, resultTwo.tagNumber)

        assertEquals(1, numTags)
    }

    @Test
    fun testProcessTags_withInvalidTag_badValue_numbers() {
        var numTags = 0
        val result = processTags("1234")
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)

        val resultTwo = processTags("101D")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("D", resultTwo.tagAlpha)
        assertEquals(101, resultTwo.tagNumber)

        assertEquals(1, numTags)
    }

    @Test
    fun testProcessTags_withBothInvalidTags() {
        var numTags = 0
        val result = processTags("NoTag")
        if (result.tagValid) {
            numTags++
        }
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)

        val resultTwo = processTags("NA")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("", resultTwo.tagAlpha)
        assertEquals(0, resultTwo.tagNumber)

        assertEquals(0, numTags)
    }

    @Test
    fun testProcessTags_withEmptyTags() {
        var numTags = 0
        val result = processTags("")
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)
        if (result.tagValid) {
            numTags++
        }
        val resultTwo = processTags("")
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("", resultTwo.tagAlpha)
        assertEquals(0, resultTwo.tagNumber)

        assertEquals(0, numTags)
    }

    @Test
    fun testProcessTags_withNullTags() {
        var numTags = 0
        val result = processTags(null)
        assertEquals("", result.tagAlpha)
        assertEquals(0, result.tagNumber)
        if (result.tagValid) {
            numTags++
        }
        val resultTwo = processTags(null)
        if (resultTwo.tagValid) {
            numTags++
        }
        assertEquals("", resultTwo.tagAlpha)
        assertEquals(0, resultTwo.tagNumber)

        assertEquals(0, numTags)    }
}