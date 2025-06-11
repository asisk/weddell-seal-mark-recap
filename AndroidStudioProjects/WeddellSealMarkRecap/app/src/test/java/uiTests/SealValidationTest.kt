package uiTests

import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.ui.utils.getTwoYearsAgo
import weddellseal.markrecap.ui.utils.getYearWithinTenYears
import org.junit.Assert
import org.junit.Test
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.ui.tagretag.sealValidation
import weddellseal.markrecap.ui.utils.getLastYear
import weddellseal.markrecap.ui.utils.getOverTenYearsAgo

class SealValidationTest {

    @Test
    fun validateNewTagEventWithWedCheckRecord() {
        val seal = Seal(tagEventType = "New", sex = "Male")
        val wedCheckSeal =
            WedCheckSeal(sex = "Male", lastSeenSeason = getYearWithinTenYears(), speNo = 12345)

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals(
            "Tag already used! Recheck all fields before saving!\nIf you choose to save this entry, please take a photo and add a comment.",
            message
        )
    }

    @Test
    fun validateRetagEventWithoutWedCheckRecord() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup")

        val (isValid, message) = sealValidation(seal, getCurrentYear(), WedCheckSeal())

        Assert.assertEquals(false, isValid)

        if (!message.contains("Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.")) {
            // fail the test if the message does not contain the expected string
            Assert.fail("Message does not contain expected string")
        }

        val markedSeal = Seal(tagEventType = "Marked", sex = "Female", age = "Pup")
        val (isValidTwo, messageTwo) = sealValidation(markedSeal, getCurrentYear(), WedCheckSeal())

        Assert.assertEquals(false, isValidTwo)

        if (!messageTwo.contains("Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.")) {
            // fail the test if the message does not contain the expected string
            Assert.fail("Message does not contain expected string")
        }
    }

    @Test
    fun validateSexMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Male", age = "Pup")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Pup",
                lastSeenSeason = getYearWithinTenYears(),
                speNo = 9637
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        if (!message.contains("Sex doesn't match")) {
            // fail the test if the message does not contain the expected string
            Assert.fail("Message does not contain expected string")
        }
    }

    @Test
    fun validateNumberOfTagsMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup", numTags = "4")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Pup",
                numTags = "2",
                lastSeenSeason = getYearWithinTenYears(),
                speNo = 1234
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        if (!message.contains("Number of tags doesn't match")) {
            // fail the test if the message does not contain the expected string
            Assert.fail("Message does not contain expected string")
        }
    }

    @Test
    fun validateSealLastSeenDead() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Male",
                age = "Pup",
                condition = "0",
                lastSeenSeason = getYearWithinTenYears(),
                speNo = 29689
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last seen dead", message.trim())
    }

    @Test
    fun validateSealLastSeenMoreThanTenYearsAgo() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Male",
                age = "Adult",
                lastSeenSeason = getOverTenYearsAgo(),
                speNo = 1234
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last seen more than ten years ago", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeWithinSameYear() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Pup",
                lastSeenSeason = getCurrentYear(),
                speNo = 1234
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last observed this year. Age class can't change!", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeFromPupToYearling() {

        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = getLastYear(), speNo = 1234)

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(true, isValid)
        Assert.assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeLastYearFromYearlingToAdult() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Yearling",
                lastSeenSeason = getLastYear(),
                speNo = 1234
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(true, isValid)
        Assert.assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassShouldBeAdultAtTwoYears() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Yearling",
                lastSeenSeason = getTwoYearsAgo(),
                speNo = 1234
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals(
            "Seal observed two or more years ago. Age class should be Adult!",
            message.trim()
        )
    }
}