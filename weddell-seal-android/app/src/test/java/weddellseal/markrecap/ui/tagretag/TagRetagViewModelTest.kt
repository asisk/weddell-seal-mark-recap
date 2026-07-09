package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private fun wedCheckRecord(
        speno: Int,
        tagId: String,
        sex: String = "M",
    ) = WedCheckRecord(
        speno = speno,
        season = getCurrentYear(),
        ageClass = "A",
        sex = sex,
        tagIdOne = tagId,
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

    /** Fix #1: [attemptSave] commits pending tag edits before writing. */
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

    /**
     * Fix #1: validation must use the committed tag (789A), not the pre-edit tag (456A) that
     * still had a WedCheck match when the user tapped Save without blurring the tag field.
     */
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
        vm.primarySeal.first { it.hasWedCheckMatch && it.isValid }

        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        vm.attemptSave(TestFixtures.sampleGeoLocation())
        vm.uiState.first { it.entryNeedsConfirmation }

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertTrue(written.isEmpty())
        assertTrue(vm.uiState.value.entryNeedsConfirmation)
        assertTrue(vm.uiState.value.validationFailureReason.contains("Seal not in database"))
    }

    /**
     * Fix #2: Marked save awaits WedCheck for the committed tag so speno is not "0" when the
     * user edits the tag number without blurring before Save.
     */
    @Test
    fun writeObservationRecord_persistsSpenoForMarkedSealAfterPendingTagEdit() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }

        val wedCheckFor789 = wedCheckRecord(speno = 42, tagId = "789A")
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("789A") } returns wedCheckFor789

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals("789A", written[0].tagIDOne)
        assertEquals("42", written[0].speno)
    }

    /** Fix #2: same speno behavior via the Confirm & Save path after validation. */
    @Test
    fun confirmAndSave_persistsSpenoForMarkedSealAfterPendingTagEdit() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }

        val wedCheckFor789 = wedCheckRecord(speno = 55, tagId = "789A")
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("789A") } returns wedCheckFor789
        every { wedCheckRepo.findSealbyTagID("456A") } returns wedCheckRecord(speno = 99, tagId = "456A")

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        vm.setIsSaving()

        vm.confirmAndSave(TestFixtures.sampleGeoLocation())
        vm.primarySeal.first { !it.isEntryStarted }

        assertEquals(1, written.size)
        assertEquals("789A", written[0].tagIDOne)
        assertEquals("55", written[0].speno)
    }

    /**
     * Fix #4: a WedCheck lookup started before [resetModelState] must not attach speno to the
     * next blank entry.
     */
    @Test
    fun findWedCheckMatch_ignoresStaleResultAfterFormReset() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor456 = wedCheckRecord(speno = 99, tagId = "456A")
        val allowStaleLookupToFinish = CompletableDeferred<Unit>()
        val wedCheckRepo = mockk<WedCheckRepository>()
        coEvery { wedCheckRepo.findSealbyTagID("456A") } coAnswers {
            allowStaleLookupToFinish.await()
            wedCheckFor456
        }

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")

        vm.findWedCheckMatch(vm.primarySeal.value, "456A")
        vm.resetModelState()
        allowStaleLookupToFinish.complete(Unit)
        yield()

        assertFalse(vm.primarySeal.value.isEntryStarted)
        assertNull(vm.primarySeal.value.wedCheckMatch)
    }

    /**
     * Fix #4: when the tag changes while a lookup is in flight, only the latest lookup may apply.
     */
    @Test
    fun findWedCheckMatch_ignoresSupersededLookupWhenTagChanges() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor456 = wedCheckRecord(speno = 99, tagId = "456A")
        val wedCheckFor789 = wedCheckRecord(speno = 42, tagId = "789A")
        val allowStaleLookupToFinish = CompletableDeferred<Unit>()
        val wedCheckRepo = mockk<WedCheckRepository>()
        coEvery { wedCheckRepo.findSealbyTagID("456A") } coAnswers {
            allowStaleLookupToFinish.await()
            wedCheckFor456
        }
        every { wedCheckRepo.findSealbyTagID("789A") } returns wedCheckFor789

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")

        vm.findWedCheckMatch(vm.primarySeal.value, "456A")
        vm.updateTagNumber(SealType.PRIMARY, "789")
        // requestCurrentWedCheckMatch skips while the first lookup holds isSearching=true,
        // so start the superseding lookup explicitly.
        vm.findWedCheckMatch(vm.primarySeal.value, "789A")
        vm.primarySeal.first { it.wedCheckMatch?.speNo == 42 }

        allowStaleLookupToFinish.complete(Unit)
        yield()

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertEquals(42, vm.primarySeal.value.wedCheckMatch?.speNo)
    }

    /**
     * Fix #4: a failed lookup for the current tag clears any stale WedCheck match instead of
     * leaving the previous tag's speno visible.
     */
    @Test
    fun findWedCheckMatch_clearsMatchWhenLookupFailsForCurrentTag() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor456 = wedCheckRecord(speno = 99, tagId = "456A")
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
        vm.primarySeal.first { it.wedCheckMatch?.speNo == 99 }

        vm.updateTagNumber(SealType.PRIMARY, "789")
        vm.primarySeal.first { !it.hasWedCheckMatch }

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertNull(vm.primarySeal.value.wedCheckMatch)
    }

    /** Fix #5: editing an existing observation appends a new row for edit history. */
    @Test
    fun writeObservationRecord_appendsNewRowWhenEditing() = runTest {
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

        val existing = TestFixtures.minimalObservationRecord().copy(
            id = 42,
            insertedAt = 1_000L,
            tagEvent = TagEventType.NEW.alpha,
            tagIDOne = "456B",
            tagOneIndicator = "+",
            sealCondition = SealCondition.GOOD.code,
        )
        vm.loadSealForEdit(DisplayObservation.Standalone(existing))
        vm.updateCondition(SealType.PRIMARY, SealCondition.FAIR)
        vm.hasEdits.first { it }

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertEquals(0, written[0].id)
        assertNull(written[0].updatedAt)
        assertEquals(SealCondition.FAIR.code, written[0].sealCondition)
    }
}
