package weddellseal.markrecap.ui.tagretag

import org.junit.Assert
import org.junit.Test
import weddellseal.markrecap.frameworks.room.Seal
import weddellseal.markrecap.frameworks.room.WedCheckSeal
import java.time.LocalDate

class SealValidationTest {

    @Test
    fun validateNewTagEventWithWedCheckRecord() {
        val seal = Seal(tagEventType = "New", sex = "Male")
        val wedCheckSeal = WedCheckSeal(sex = "Male", lastSeenSeason = getYearWithinTenYears())

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
        Assert.assertEquals(
            "Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.",
            message
        )

        val markedSeal = Seal(tagEventType = "Marked", sex = "Female", age = "Pup")
        val (isValidTwo, messageTwo) = sealValidation(markedSeal, getCurrentYear(), WedCheckSeal())

        Assert.assertEquals(false, isValidTwo)
        Assert.assertEquals(
            "Seal not in database!\nIf you choose to save this entry, please take a photo and add a comment.",
            messageTwo
        )
    }

    @Test
    fun validateSexMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup")
        val wedCheckSeal =
            WedCheckSeal(sex = "Male", age = "Pup", lastSeenSeason = getYearWithinTenYears())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Sex doesn't match", message.trim())
    }

    @Test
    fun validateNumberOfTagsMismatch() {
        val seal = Seal(tagEventType = "Retag", sex = "Female", age = "Pup", numTags = "4")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Female",
                age = "Pup",
                numTags = "2",
                lastSeenSeason = getYearWithinTenYears()
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Number of tags doesn't match", message.trim())
    }

    @Test
    fun validateSealLastSeenDead() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(
                sex = "Male",
                age = "Adult",
                condition = "Dead",
                lastSeenSeason = getYearWithinTenYears()
            )

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last seen dead", message.trim())
    }

    @Test
    fun validateSealLastSeenMoreThanTenYearsAgo() {
        val seal = Seal(tagEventType = "Marked", sex = "Male", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(sex = "Male", age = "Adult", lastSeenSeason = getOverTenYearsAgo())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last seen more than ten years ago", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeWithinSameYear() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = getCurrentYear())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Seal last observed this year. Age class can't change!", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeFromPupToYearling() {

        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal = WedCheckSeal(sex = "Female", age = "Pup", lastSeenSeason = getLastYear())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(true, isValid)
        Assert.assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassChangeLastYearFromYearlingToAdult() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Adult")
        val wedCheckSeal =
            WedCheckSeal(sex = "Female", age = "Yearling", lastSeenSeason = getLastYear())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(true, isValid)
        Assert.assertEquals("", message.trim())
    }

    @Test
    fun validateSealAgeClassShouldBeAdultAtTwoYears() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(sex = "Female", age = "Yearling", lastSeenSeason = getTwoYearsAgo())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals(
            "Seal observed two years ago. Age class should be adult!",
            message.trim()
        )
    }

    @Test
    fun validateSealAgeClassShouldMatchAfterTwoYears() {
        val seal = Seal(tagEventType = "Marked", sex = "Female", age = "Yearling")
        val wedCheckSeal =
            WedCheckSeal(sex = "Female", age = "Adult", lastSeenSeason = getYearWithinTenYears())

        val (isValid, message) = sealValidation(seal, getCurrentYear(), wedCheckSeal)

        Assert.assertEquals(false, isValid)
        Assert.assertEquals("Age class should match.", message.trim())
    }
}

private fun getCurrentYear(): Int {
    return LocalDate.now().year
}

private fun getLastYear(): Int {
    return LocalDate.now().year - 1
}

private fun getTwoYearsAgo(): Int {
    return LocalDate.now().year - 2
}

private fun getYearWithinTenYears(): Int {
    return LocalDate.now().year - 6
}

private fun getOverTenYearsAgo(): Int {
    return LocalDate.now().year - 12
}