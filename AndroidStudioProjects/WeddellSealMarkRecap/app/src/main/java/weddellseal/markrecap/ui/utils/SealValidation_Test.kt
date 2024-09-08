package weddellseal.markrecap.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import weddellseal.markrecap.data.Seal
import weddellseal.markrecap.data.WedCheckSeal


class SealValidation_Test {

    @Test
    fun validateNewTagEventWithWedCheckRecord() {
        val seal = Seal(tagEventType = "New", sex = "Male")
        val wedCheckSeal = WedCheckSeal(sex = "Male")

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals(
            "Tag already used! Recheck all fields before saving!\nIf you choose to save this entry, please take a photo and add a comment.",
            message
        )
    }

    @Test
    fun validateRetagEventWithoutWedCheckRecord() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup")

        val (isValid, message) = sealValidation(seal, 2024, null)

        assertEquals(false, isValid)
        assertEquals(
            "Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.",
            message
        )

        val markedSeal = Seal(tagEventType = "Marked", sex = "Female", age = "Pup")
        val (isValidTwo, messageTwo) = sealValidation(markedSeal, 2024, null)

        assertEquals(false, isValidTwo)
        assertEquals(
            "Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.",
            messageTwo
        )
    }

    @Test
    fun validateSexMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup")
        val wedCheckSeal = WedCheckSeal(sex = "Male", age = "Pup")

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Sex doesn't match", message.trim())
    }

    @Test
    fun validateNumberOfTagsMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = 2023)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Number of tags doesn't match", message.trim())
    }

    @Test
    fun validateSealLastSeenDead() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(sex = "Male", age = "Adult", condition = "Dead", lastSeenSeason = 2022)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Seal last seen dead", message.trim())
    }

    @Test
    fun validateSealLastSeenMoreThanTenYearsAgo() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal = WedCheckSeal(sex = "Male", age = "Adult", lastSeenSeason = 2013)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Seal last seen more than ten years ago", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeWithinSameYear() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = 2024)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Seal last observed this year. Age class can't change!", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeLastYearFromPupToYearling() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = 2023)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(true, isValid)
        assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeLastYearFromYearlingToAdult() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Adult")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Yearling", lastSeenSeason = 2023)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(true, isValid)
        assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassShouldBeAdultAfterTwoYears() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Yearling", lastSeenSeason = 2022)

        val (isValid, message) = sealValidation(seal, 2024, wedCheckSeal)

        assertEquals(false, isValid)
        assertEquals("\n Seal observed two years ago. Age class should be adult!", message.trim())
    }
}