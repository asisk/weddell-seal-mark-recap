package weddellseal.markrecap

import weddellseal.markrecap.domain.location.data.Coordinates
import weddellseal.markrecap.domain.location.data.GeoLocation
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.ui.home.ObservationMetadata

object TestFixtures {

    fun sampleColony(
        location: String = "TestColony",
        fileUploadId: Long = 1L,
    ) = SealColony(
        inOut = "in",
        location = location,
        nLimit = 45.0,
        sLimit = 40.0,
        wLimit = 30.0,
        eLimit = 35.0,
        adjLat = -77.5,
        adjLong = 166.5,
        fileUploadId = fileUploadId
    )

    fun sampleMetadata(colony: SealColony = sampleColony()) = ObservationMetadata(
        selectedColony = colony,
        selectedObservers = listOf("JD"),
        censusNumber = "",
        isCensusMode = false,
        deviceID = "test-device",
        currentSeason = "2025",
    )

    fun sampleGeoLocation() = GeoLocation(
        coordinates = Coordinates(latitude = -77.1234, longitude = 166.5678)
    )

    fun completePrimaryMarkedSeal() = Seal(
        sealType = SealType.PRIMARY,
        ageClass = SealAgeClass.ADULT,
        sex = SealSex.FEMALE,
        numRelatives = SealRelatives.ZERO,
        tagEventType = TagEventType.MARKED,
        tagNumber = "123",
        tagAlpha = "A",
        numTags = "1",
        condition = SealCondition.GOOD,
    )

    fun completePrimaryNewSeal() = Seal(
        sealType = SealType.PRIMARY,
        ageClass = SealAgeClass.ADULT,
        sex = SealSex.MALE,
        numRelatives = SealRelatives.ZERO,
        tagEventType = TagEventType.NEW,
        tagNumber = "456",
        tagAlpha = "B",
        numTags = "1",
        condition = SealCondition.FAIR,
    )

    fun minimalObservationRecord() = ObservationRecord(
        id = 0,
        deviceID = "dev",
        season = "2025",
        speno = "0",
        date = "2025-01-01",
        time = "12:00:00",
        censusID = "0",
        latitude = "-77.0",
        longitude = "166.0",
        ageClass = SealAgeClass.ADULT.alpha,
        sex = SealSex.FEMALE.alpha,
        numRelatives = "0",
        oldTagIDOne = "",
        oldTagIDTwo = "",
        tagIDOne = "123A",
        tagOneIndicator = "",
        tagIDTwo = "NoTag",
        tagTwoIndicator = "",
        relativeTagIDOne = "",
        relativeTagIDTwo = "",
        sealCondition = SealCondition.GOOD.code,
        observerInitials = "JD",
        flaggedEntry = "",
        tagEvent = TagEventType.MARKED.alpha,
        weight = "",
        tissueSampled = "",
        comments = "",
        retagReason = "",
        colony = "ColonyX",
    )
}
