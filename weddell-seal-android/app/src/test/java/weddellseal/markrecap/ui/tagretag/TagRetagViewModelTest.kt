package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import weddellseal.markrecap.TestFixtures
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.ui.utils.getCurrentYear

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class TagRetagViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onViewAttempt_setsSelectedObservationBeforeNavigation() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)
        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        val record = TestFixtures.minimalObservationRecord()
        val displayObs = DisplayObservation.Standalone(record)

        vm.onViewAttempt(displayObs)

        assertEquals(displayObs, vm.selectedRecentObservation.value)
    }

    @Test
    fun writeObservationRecord_persistsNewSeal() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")

        assertTrue(vm.uiState.value.isSaveEnabled)

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals("456B", written[0].tagIDOne)
        assertEquals(TagEventType.NEW.alpha, written[0].tagEvent)
    }

    /**
     * Models: user entered tag 456B, moved to another field (committed), then returned to Tag ID,
     * changed it to 789B, and tapped Save without blurring the Tag ID field. The ViewModel still
     * holds 456B because [TagIdSection] only commits on focus loss.
     */
    @Test
    fun writeObservationRecord_persistsReEditedTagNumberWithoutRequiringBlur() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")

        // User re-edits Tag ID in the UI to 789B but Save is tapped before the field blurs,
        // so updateTagNumber is never called with "789" (only updatePendingTagNumber, on each keystroke).
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        assertEquals("456", vm.primarySeal.value.tagNumber)

        assertTrue(vm.uiState.value.isSaveEnabled)
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals(
            "Save should persist the in-progress Tag ID edit even when the field still has focus",
            "789B",
            written[0].tagIDOne,
        )
    }

    @Test
    fun attemptSave_persistsPendingTagNumberWithoutRequiringBlur() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")

        assertTrue(vm.uiState.value.isSaveEnabled)
        vm.attemptSave(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals("789B", written[0].tagIDOne)
    }

    @Test
    fun attemptSave_validatesUsingCommittedTagNotStaleModelSnapshot() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }

        val wedCheckFor456 = WedCheckRecord(
            speno = 99,
            season = getCurrentYear(),
            ageClass = "A",
            sex = "M",
            tagIdOne = "456A",
            tagIdTwo = "NA",
            comments = "",
            ageYears = 3,
            tissueSampled = "NA",
            pupinMassStudy = "NA",
            numPreviousPups = "NA",
            pupinTTStudy = "NA",
            momMassMeasurements = "NA",
            condition = "3",
            lastPhysio = "NA",
            population = "NA",
            fileUploadId = 1L,
            latitude = -77.0,
            longitude = 166.0,
        )
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("456A") } returns wedCheckFor456
        every { wedCheckRepo.findSealbyTagID("789A") } throws NoSuchElementException()

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.findWedCheckMatch(vm.primarySeal.value, "456A")
        runBlocking { delay(100) }

        assertTrue(vm.primarySeal.value.hasWedCheckMatch)
        assertTrue(vm.uiState.value.allSealsValid)

        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        vm.attemptSave(TestFixtures.sampleGeoLocation())

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertTrue(written.isEmpty())
        assertTrue(vm.uiState.value.entryNeedsConfirmation)
        assertTrue(vm.uiState.value.validationFailureReason.contains("Seal not in database"))
    }
}
