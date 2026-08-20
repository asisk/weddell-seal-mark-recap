package weddellseal.markrecap.ui.tagretag

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observations.ObservationRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.ui.recentobservations.observationsToDisplay
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueObservation
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
        tagIdTwo: String = "NA",
        season: Int = getCurrentYear(),
        ageYears: Int = 3,
        condition: String = "3",
        tissueSampled: String = "NA",
        lastPhysio: String = "NA",
        population: String = "NA",
        latitude: Double = -77.0,
        longitude: Double = 166.0,
    ) = WedCheckRecord(
        speno = speno,
        season = season,
        ageClass = "A",
        sex = sex,
        tagIdOne = tagId,
        tagIdTwo = tagIdTwo,
        comments = "",
        ageYears = ageYears,
        tissueSampled = tissueSampled,
        pupinMassStudy = "NA",
        numPreviousPups = "NA",
        pupinTTStudy = "NA",
        momMassMeasurements = "NA",
        condition = condition,
        lastPhysio = lastPhysio,
        population = population,
        fileUploadId = 1L,
        latitude = latitude,
        longitude = longitude,
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
     * Reported sequence: Retag + Old Tag Marks, then switch to New or Marked.
     * Matches SealCard: onRetagDeselection then updateTagEventType.
     */
    @Test
    fun writeObservationRecord_omitsOldTagMarksCommentAfterSwitchingFromRetag() = runTest {
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

        listOf(TagEventType.NEW, TagEventType.MARKED).forEach { correctedEvent ->
            written.clear()

            vm.prefillSingleMale()
            vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
            vm.updateTagNumber(SealType.PRIMARY, "456")
            vm.updateTagAlpha(SealType.PRIMARY, "B")
            vm.updateNumTags(SealType.PRIMARY, "1")
            vm.updateTagEventType(vm.primarySeal.value, TagEventType.RETAG)
            vm.onRetagSelection(SealType.PRIMARY, "456", "B")
            vm.updateOldTagMarks(SealType.PRIMARY, true)
            assertTrue(vm.primarySeal.value.oldTagMarks)
            assertEquals(TagEventType.RETAG, vm.primarySeal.value.tagEventType)

            vm.onRetagDeselection(
                SealType.PRIMARY,
                vm.primarySeal.value.oldTagNumber,
                vm.primarySeal.value.oldTagAlpha,
            )
            vm.updateTagEventType(vm.primarySeal.value, correctedEvent)
            assertFalse(
                "oldTagMarks should be cleared when switching from Retag to $correctedEvent",
                vm.primarySeal.value.oldTagMarks,
            )

            vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

            assertEquals(1, written.size)
            assertFalse(
                "comments should not contain old tag marks after switching to $correctedEvent",
                written[0].comments.contains("old tag marks"),
            )
        }
    }

    @Test
    fun updateTagEventType_keepsOldTagMarksWhenSwitchingFromNewToRetag() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)
        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateOldTagMarks(SealType.PRIMARY, true)

        vm.updateTagEventType(vm.primarySeal.value, TagEventType.RETAG)
        assertTrue(vm.primarySeal.value.oldTagMarks)
    }

    @Test
    fun writeObservationRecord_includesOldTagMarksCommentForNewEvent() = runTest {
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
        vm.updateOldTagMarks(SealType.PRIMARY, true)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertTrue(written[0].comments.contains("old tag marks; "))
    }

    /**
     * Models: user entered tag 456B, moved to another field (committed), then returned to Tag ID,
     * changed it to 789B, and tapped Save without blurring the Tag ID field. The ViewModel still
     * holds 456B because [TagIdSection] only commits on focus loss.
     *
     * Parker 2025 season recap: Speno / tag did not refresh unless the field lost focus.
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
        // NEW tags must not resolve a WedCheck match; a relaxed mock can return a non-null
        // record asynchronously and send attemptSave down the confirmation path instead of write.
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID(any()) } throws NoSuchElementException()

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
        yield()

        assertEquals(1, written.size)
        assertEquals("789B", written[0].tagIDOne)
        assertFalse(vm.uiState.value.entryNeedsConfirmation)
    }

    /**
     * CommentField keeps text local until blur. Save without leaving the field must still
     * persist the in-progress comment on the current observation (not the next blank seal).
     */
    @Test
    fun writeObservationRecord_persistsPendingCommentWithoutRequiringBlur() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID(any()) } throws NoSuchElementException()

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")

        // Typed in the comment box but Save before blur, so seal.comment is still empty.
        vm.updatePendingComment(SealType.PRIMARY, "scar on left flipper")
        assertEquals("", vm.primarySeal.value.comment)

        assertTrue(vm.uiState.value.isSaveEnabled)
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(1, written.size)
        assertTrue(
            "Save should persist the in-progress comment even when the field still has focus",
            written[0].comments.contains("scar on left flipper"),
        )
        assertEquals(
            "After save, the next seal entry must not inherit the previous comment",
            "",
            vm.primarySeal.value.comment,
        )
    }

    @Test
    fun attemptSave_persistsPendingCommentWithoutRequiringBlur() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        // NEW tags must not resolve a WedCheck match; a relaxed mock can return a non-null
        // record asynchronously and send attemptSave down the confirmation path instead of write.
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID(any()) } throws NoSuchElementException()

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.NEW)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "B")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.updatePendingComment(SealType.PRIMARY, "scar on left flipper")

        assertTrue(vm.uiState.value.isSaveEnabled)
        vm.attemptSave(TestFixtures.sampleGeoLocation())
        yield()

        assertEquals(1, written.size)
        assertTrue(written[0].comments.contains("scar on left flipper"))
        assertEquals("", vm.primarySeal.value.comment)
    }

    @Test
    fun updateCommentIfCurrent_ignoresStaleBlurAfterFormReset() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)
        val counterBeforeReset = vm.uiState.value.fieldResetCounter

        vm.resetModelState()
        assertTrue(vm.uiState.value.fieldResetCounter > counterBeforeReset)

        // Deferred blur from the previous CommentField instance after save/reset.
        vm.updateCommentIfCurrent(SealType.PRIMARY, "should not stick", counterBeforeReset)
        assertEquals("", vm.primarySeal.value.comment)

        vm.updateCommentIfCurrent(
            SealType.PRIMARY,
            "fresh note",
            vm.uiState.value.fieldResetCounter,
        )
        assertEquals("fresh note", vm.primarySeal.value.comment)
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
     * Parker 2025 season recap: false "are you sure this is a male" flash when Save ran
     * against a stale Speno before WedCheck finished. [isSaveAttempted] (which gates the
     * validation banner) must stay false until the save-path lookup completes. After refresh
     * the committed tag matches the entered sex, so the entry saves with no confirmation.
     */
    @Test
    fun attemptSave_doesNotSetSaveAttemptedUntilWedCheckRefreshCompletes() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }

        val staleFemaleMatch = wedCheckRecord(speno = 99, tagId = "456A", sex = "F")
        val committedMaleMatch = wedCheckRecord(speno = 42, tagId = "789A", sex = "M")
        val lookupStarted = CompletableDeferred<Unit>()
        val allowLookupToFinish = CompletableDeferred<Unit>()
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("456A") } returns staleFemaleMatch
        coEvery { wedCheckRepo.findSealbyTagID("789A") } coAnswers {
            lookupStarted.complete(Unit)
            allowLookupToFinish.await()
            committedMaleMatch
        }

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.findWedCheckMatch(vm.primarySeal.value, "456A")
        vm.primarySeal.first { it.hasWedCheckMatch }

        assertTrue(
            "Stale female WedCheck vs entered male should already be a sex mismatch",
            vm.primarySeal.value.validationErrors.any { it.contains("Sex doesn't match") },
        )

        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        vm.attemptSave(TestFixtures.sampleGeoLocation())
        lookupStarted.await()

        assertFalse(
            "Validation banner is gated on isSaveAttempted; it must stay false while Speno refresh is in flight",
            vm.uiState.value.isSaveAttempted,
        )
        assertTrue(written.isEmpty())
        assertFalse(vm.uiState.value.entryNeedsConfirmation)

        allowLookupToFinish.complete(Unit)
        vm.primarySeal.first { !it.isEntryStarted }

        assertEquals(1, written.size)
        assertEquals("789A", written[0].tagIDOne)
        assertEquals("42", written[0].speno)
        assertFalse(vm.uiState.value.entryNeedsConfirmation)
    }

    /**
     * Parker 2025 season recap: Speno did not refresh unless the tag field lost focus.
     * 4-digit tag numbers look up Speno while typing.
     */
    @Test
    fun updatePendingTagNumber_fourDigits_looksUpWedCheckWithoutBlur() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor1234 = wedCheckRecord(speno = 42, tagId = "1234A")
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("1234A") } returns wedCheckFor1234

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")
        vm.updatePendingTagNumber(SealType.PRIMARY, "1234")

        assertEquals("1234", vm.primarySeal.value.tagNumber)
        vm.primarySeal.first { it.hasWedCheckMatch }
        assertEquals(42, vm.primarySeal.value.wedCheckMatch?.speNo)
    }

    @Test
    fun updatePendingTagNumber_threeDigits_doesNotCommitUntilBlurOrSave() = runTest {
        // Parker 2025 season recap: 3-digit tags still commit on blur or Save (last seen ~2 years
        // before 2025); only 4-digit tags look up Speno while typing.
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)
        val wedCheckRepo = mockk<WedCheckRepository>(relaxed = true)

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")

        assertEquals("456", vm.primarySeal.value.tagNumber)
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
        // Changing the tag while a lookup is in flight must still request the new match;
        // Fix #4's counter discards the stale 456A result.
        vm.primarySeal.first { it.wedCheckMatch?.speNo == 42 }

        allowStaleLookupToFinish.complete(Unit)
        yield()

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertEquals(42, vm.primarySeal.value.wedCheckMatch?.speNo)
    }

    /**
     * Live SPENO: selecting tag alpha while a previous WedCheck lookup is still in flight
     * must start a new lookup for the updated tag ID (not skip because isSearching).
     */
    @Test
    fun requestCurrentWedCheckMatch_startsLookupWhilePreviousSearchInFlight() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor456 = wedCheckRecord(speno = 99, tagId = "456A")
        val wedCheckFor456C = wedCheckRecord(speno = 42, tagId = "456C")
        val allowFirstLookupToFinish = CompletableDeferred<Unit>()
        val wedCheckRepo = mockk<WedCheckRepository>()
        coEvery { wedCheckRepo.findSealbyTagID("456A") } coAnswers {
            allowFirstLookupToFinish.await()
            wedCheckFor456
        }
        every { wedCheckRepo.findSealbyTagID("456C") } returns wedCheckFor456C

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "456")
        vm.updateTagAlpha(SealType.PRIMARY, "A")
        vm.updateNumTags(SealType.PRIMARY, "1")

        // Hold the first lookup open, then correct the alpha — SPENO must update to 42.
        vm.findWedCheckMatch(vm.primarySeal.value, "456A")
        vm.updateTagAlpha(SealType.PRIMARY, "C")
        vm.primarySeal.first { it.wedCheckMatch?.speNo == 42 }

        allowFirstLookupToFinish.complete(Unit)
        yield()

        assertEquals("C", vm.primarySeal.value.tagAlpha)
        assertEquals(42, vm.primarySeal.value.wedCheckMatch?.speNo)
    }

    /**
     * Live SPENO: alpha selection commits a pending tag number so WedCheck can run without
     * waiting for an explicit blur commit.
     */
    @Test
    fun updateTagAlpha_commitsPendingTagNumberAndLooksUpSpeno() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        val wedCheckFor789 = wedCheckRecord(speno = 42, tagId = "789A")
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("789A") } returns wedCheckFor789

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateNumTags(SealType.PRIMARY, "1")
        // Number is typed but not yet committed to the seal model (blur has not fired).
        vm.updatePendingTagNumber(SealType.PRIMARY, "789")
        vm.updateTagAlpha(SealType.PRIMARY, "A")

        vm.primarySeal.first { it.wedCheckMatch?.speNo == 42 }

        assertEquals("789", vm.primarySeal.value.tagNumber)
        assertEquals("A", vm.primarySeal.value.tagAlpha)
        assertEquals(42, vm.primarySeal.value.wedCheckMatch?.speNo)
    }

    /**
     * WedCheck stores two physical tags per seal (CSV tag1 / tag2). The Tag/Retag UI only
     * lets the technician enter one tag ID, but that ID may match tag2 on the WedCheck row.
     *
     * Grounded in WedCheckFull_withLatLong.csv:
     *   speno=6419, tag1=657A, tag2=658A, sex=M, last_seen=1999
     *
     * After a match found via tag2, [requestCurrentWedCheckMatch] must treat the match as
     * current (check tagIdTwo, not only tagIdOne) and not clear/re-fetch.
     */
    @Test
    fun requestCurrentWedCheckMatch_keepsMatchWhenEnteredTagIsWedCheckTagTwo() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>(relaxed = true)

        // Real WedCheck row: looking up 658A returns a record whose tagIdOne is still 657A.
        val wedCheckSpeno6419 = wedCheckRecord(
            speno = 6419,
            tagId = "657A",
            tagIdTwo = "658A",
            sex = "M",
            season = 1999,
            ageYears = 41,
            condition = "NA",
            tissueSampled = "Need",
            lastPhysio = "1999",
            population = "Erebus Bay",
            latitude = -77.744,
            longitude = 166.77,
        )
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID("658A") } returns wedCheckSpeno6419
        every { wedCheckRepo.findSealbyTagID("657A") } returns wedCheckSpeno6419

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateNumTags(SealType.PRIMARY, "2")
        // Enter the second physical tag only (what the technician reads in the field).
        vm.updateTagNumber(SealType.PRIMARY, "658")
        vm.updateTagAlpha(SealType.PRIMARY, "A")

        vm.primarySeal.first { it.wedCheckMatch?.speNo == 6419 }

        assertEquals("658", vm.primarySeal.value.tagNumber)
        assertEquals("657A", vm.primarySeal.value.wedCheckMatch?.tagIdOne)
        assertEquals("658A", vm.primarySeal.value.wedCheckMatch?.tagIdTwo)
        verify(exactly = 1) { wedCheckRepo.findSealbyTagID("658A") }

        // Same tag still entered — must keep the match without another lookup.
        vm.requestCurrentWedCheckMatch(vm.primarySeal.value)

        assertEquals(6419, vm.primarySeal.value.wedCheckMatch?.speNo)
        verify(exactly = 1) { wedCheckRepo.findSealbyTagID("658A") }
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

    /**
     * Parker 2025 season recap: dummy 0000D skips WedCheck validation so Save does not
     * require confirmation for "Seal not in database!".
     */
    @Test
    fun attemptSave_markedDummyTag0000D_savesWithoutConfirmation() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val written = mutableListOf<ObservationRecord>()
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        val wedCheckRepo = mockk<WedCheckRepository>()
        every { wedCheckRepo.findSealbyTagID(any()) } throws NoSuchElementException()

        val vm = TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)

        vm.prefillSingleMale()
        vm.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        vm.updateTagEventType(vm.primarySeal.value, TagEventType.MARKED)
        vm.updateTagNumber(SealType.PRIMARY, "0000")
        vm.updateTagAlpha(SealType.PRIMARY, "D")
        vm.updateNumTags(SealType.PRIMARY, "1")

        assertTrue(vm.primarySeal.value.isDummyTag)
        assertTrue(vm.uiState.value.isSaveEnabled)
        vm.attemptSave(TestFixtures.sampleGeoLocation())
        yield()

        assertEquals(1, written.size)
        assertFalse(vm.uiState.value.entryNeedsConfirmation)
        assertEquals("", vm.uiState.value.validationFailureReason)
        verify(exactly = 0) { wedCheckRepo.findSealbyTagID("0000D") }
    }

    /**
     * Parker 2025 season recap: editing an entry created fake untagged pups. Dummy tag 0000D
     * mom/pup pairs must not write a ghost "P No Tag" row. First save is fine; the ghost
     * appeared only after edit, whether mom or pup is the dummy and whether mom or pup is
     * the animal being edited.
     */
    @Test
    fun writeObservationRecord_editMomWhenMomIsDummy0000D_doesNotCreateGhostNoTagPup() = runTest {
        assertEditMomPupWithDummyDoesNotCreateGhostPup(
            dummyOnMom = true,
            editPup = false,
        )
    }

    @Test
    fun writeObservationRecord_editPupWhenMomIsDummy0000D_doesNotCreateGhostNoTagPup() = runTest {
        assertEditMomPupWithDummyDoesNotCreateGhostPup(
            dummyOnMom = true,
            editPup = true,
        )
    }

    @Test
    fun writeObservationRecord_editMomWhenPupIsDummy0000D_doesNotCreateGhostNoTagPup() = runTest {
        assertEditMomPupWithDummyDoesNotCreateGhostPup(
            dummyOnMom = false,
            editPup = false,
        )
    }

    @Test
    fun writeObservationRecord_editPupWhenPupIsDummy0000D_doesNotCreateGhostNoTagPup() = runTest {
        assertEditMomPupWithDummyDoesNotCreateGhostPup(
            dummyOnMom = false,
            editPup = true,
        )
    }

    /**
     * Edit-save currently writes any seal with [weddellseal.markrecap.domain.tagretag.data.Seal.hasEdits],
     * including the unused pup-two slot (age P, empty tags → displayed as "P No Tag").
     */
    @Test
    fun writeObservationRecord_editDoesNotPersistUnusedPupTwoSlot() = runTest {
        val written = mutableListOf<ObservationRecord>()
        val vm = tagRetagViewModel(written)

        vm.enterMomAndPup(momTag = "1234" to "A", pupTag = "0000" to "D")
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        val mom = written.single { it.ageClass == SealAgeClass.ADULT.alpha }.copy(id = 1)
        val pup = written.single { it.ageClass == SealAgeClass.PUP.alpha }.copy(id = 2)
        written.clear()

        vm.loadSealForEdit(DisplayObservation.WithPups(mom, pup, pupTwo = null))
        vm.updateCondition(SealType.PRIMARY, SealCondition.FAIR)
        vm.updateCondition(SealType.PUPTWO, SealCondition.GOOD)
        vm.hasEdits.first { it }

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertTrue(
            "Edit wrote a ghost pup: ${written.map { notebookEntryValueObservation(it) }}",
            written.none { it.isGhostNoTagPup() },
        )
        assertTrue(written.none { it.ageClass == SealAgeClass.PUP.alpha && it.id == 0 && it.tagIDOne.isBlank() })
    }

    /**
     * Relatives are locked in the UI when a pup already exists, but the write path must still
     * persist a complete pup two if number of relatives is raised to two during edit.
     */
    @Test
    fun writeObservationRecord_addSecondPupDuringEdit_persistsPupTwo() = runTest {
        val written = mutableListOf<ObservationRecord>()
        val vm = tagRetagViewModel(written)

        vm.enterMomAndPup(momTag = "1234" to "A", pupTag = "5678" to "B")
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        val mom = written.single { it.ageClass == SealAgeClass.ADULT.alpha }.copy(id = 1)
        val pupOne = written.single { it.ageClass == SealAgeClass.PUP.alpha }.copy(id = 2)
        written.clear()

        vm.loadSealForEdit(DisplayObservation.WithPups(mom, pupOne, pupTwo = null))
        vm.updateNumRelatives(SealRelatives.TWO)
        vm.fillPup(
            sealType = SealType.PUPTWO,
            tag = "9012" to "C",
        )
        vm.hasEdits.first { it }

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertTrue(written.none { it.isGhostNoTagPup() })
        val savedPupTwo = written.single { it.tagIDOne == "9012C" }
        assertEquals(SealAgeClass.PUP.alpha, savedPupTwo.ageClass)
        assertEquals("2", savedPupTwo.numRelatives)
    }

    /** A complete No Tag pup is a real relative, not the incomplete ghost "P No Tag" row. */
    @Test
    fun writeObservationRecord_editRealNoTagPup_persistsNoTagPup() = runTest {
        val written = mutableListOf<ObservationRecord>()
        val vm = tagRetagViewModel(written)

        vm.enterMomAndPup(momTag = "1234" to "A", pupTag = null, pupNoTag = true)
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        val mom = written.single { it.ageClass == SealAgeClass.ADULT.alpha }.copy(id = 1)
        val pup = written.single { it.ageClass == SealAgeClass.PUP.alpha }.copy(id = 2)
        assertEquals("NoTag", pup.tagIDOne)
        assertEquals(TagEventType.MARKED.alpha, pup.tagEvent)
        written.clear()

        vm.loadSealForEdit(DisplayObservation.WithPups(mom, pup, pupTwo = null))
        assertTrue(vm.pupOne.value.isNoTag)
        vm.updateCondition(SealType.PUPONE, SealCondition.FAIR)
        vm.hasEdits.first { it }

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertTrue(written.none { it.isGhostNoTagPup() })
        val savedPup = written.single { it.ageClass == SealAgeClass.PUP.alpha }
        assertEquals("NoTag", savedPup.tagIDOne)
        assertEquals(TagEventType.MARKED.alpha, savedPup.tagEvent)
        assertEquals(SealCondition.FAIR.code, savedPup.sealCondition)
        assertTrue(savedPup.sex.isNotBlank())
    }

    /**
     * Recent Observations is newest-first. Grouping attaches the first 0000D pup in that list,
     * so Edit opens whatever grouping selected — including a later dummy pup from another entry.
     */
    @Test
    fun loadSealForEdit_momPup0000D_usesPupGroupingAttachedFromRecentObservations() = runTest {
        val laterDummyPup = TestFixtures.minimalObservationRecord().copy(
            id = 10,
            ageClass = SealAgeClass.PUP.alpha,
            sex = SealSex.UNKNOWN.alpha,
            numRelatives = "1",
            tagIDOne = "0000D",
            relativeTagIDOne = "9999Z",
        )
        val pairPup = TestFixtures.minimalObservationRecord().copy(
            id = 2,
            ageClass = SealAgeClass.PUP.alpha,
            sex = SealSex.UNKNOWN.alpha,
            numRelatives = "1",
            tagIDOne = "0000D",
            relativeTagIDOne = "1234A",
        )
        val mom = TestFixtures.minimalObservationRecord().copy(
            id = 1,
            ageClass = SealAgeClass.ADULT.alpha,
            sex = SealSex.FEMALE.alpha,
            numRelatives = "1",
            tagIDOne = "1234A",
            relativeTagIDOne = "0000D",
        )

        val grouped = observationsToDisplay(listOf(laterDummyPup, pairPup, mom))
        val row = grouped.filterIsInstance<DisplayObservation.WithPups>().single {
            it.primarySeal.id == 1
        }
        assertEquals(
            "Newest-first list attaches the later 0000D pup, not this mom's original pup",
            10,
            row.pupOne?.id,
        )

        val written = mutableListOf<ObservationRecord>()
        val vm = tagRetagViewModel(written)
        vm.loadSealForEdit(row)

        assertEquals(10, vm.pupOne.value.observationID)
        assertEquals("0000", vm.pupOne.value.tagNumber)
        assertEquals("D", vm.pupOne.value.tagAlpha)
        assertEquals("9999Z", laterDummyPup.relativeTagIDOne)
    }

    private suspend fun assertEditMomPupWithDummyDoesNotCreateGhostPup(
        dummyOnMom: Boolean,
        editPup: Boolean,
    ) {
        val written = mutableListOf<ObservationRecord>()
        val vm = tagRetagViewModel(written)

        val momTag = if (dummyOnMom) "0000" to "D" else "1234" to "A"
        val pupTag = if (dummyOnMom) "5678" to "B" else "0000" to "D"
        vm.enterMomAndPup(momTag = momTag, pupTag = pupTag)
        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertEquals(2, written.size)
        assertTrue(written.none { it.isGhostNoTagPup() })

        val mom = written.single { it.ageClass == SealAgeClass.ADULT.alpha }.copy(id = 1)
        val pup = written.single { it.ageClass == SealAgeClass.PUP.alpha }.copy(id = 2)
        written.clear()

        vm.loadSealForEdit(DisplayObservation.WithPups(mom, pup, pupTwo = null))
        if (editPup) {
            vm.updateCondition(SealType.PUPONE, SealCondition.FAIR)
        } else {
            vm.updateCondition(SealType.PRIMARY, SealCondition.FAIR)
        }
        vm.hasEdits.first { it }

        vm.writeObservationRecord(TestFixtures.sampleGeoLocation())

        assertTrue(
            "Edit wrote a ghost pup: ${written.map { notebookEntryValueObservation(it) }}",
            written.none { it.isGhostNoTagPup() },
        )
        assertTrue(written.any { it.ageClass == if (editPup) SealAgeClass.PUP.alpha else SealAgeClass.ADULT.alpha })
    }

    private fun tagRetagViewModel(
        written: MutableList<ObservationRecord>,
        wedCheckRepo: WedCheckRepository = mockk(relaxed = true),
    ): TagRetagViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val metadata = MutableStateFlow(TestFixtures.sampleMetadata())
        val homeUi = MutableStateFlow(HomeViewModel.UiState(overrideColony = false))
        val observationRepo = mockk<ObservationRepository>()
        coEvery { observationRepo.writeObservation(any()) } answers {
            written.add(firstArg())
        }
        coEvery { observationRepo.deleteObservation(any()) } returns Unit
        return TagRetagViewModel(app, observationRepo, wedCheckRepo, metadata, homeUi)
    }

    private fun TagRetagViewModel.enterMomAndPup(
        momTag: Pair<String, String>,
        pupTag: Pair<String, String>?,
        pupNoTag: Boolean = false,
    ) {
        prefillMomAndPup()
        updateCondition(SealType.PRIMARY, SealCondition.GOOD)
        updateTagEventType(primarySeal.value, TagEventType.NEW)
        updateTagNumber(SealType.PRIMARY, momTag.first)
        updateTagAlpha(SealType.PRIMARY, momTag.second)
        updateNumTags(SealType.PRIMARY, "1")

        fillPup(SealType.PUPONE, tag = pupTag, noTag = pupNoTag)
    }

    private fun TagRetagViewModel.fillPup(
        sealType: SealType,
        tag: Pair<String, String>?,
        noTag: Boolean = false,
    ) {
        val eventSeal = when (sealType) {
            SealType.PUPONE -> pupOne.value
            SealType.PUPTWO -> pupTwo.value
            else -> primarySeal.value
        }
        updateSex(sealType, SealSex.UNKNOWN)
        updateCondition(sealType, SealCondition.NEWBORN)
        if (noTag) {
            updateTagEventType(eventSeal, TagEventType.MARKED)
            updateNoTag(sealType, true)
        } else {
            requireNotNull(tag)
            updateTagEventType(eventSeal, TagEventType.NEW)
            updateTagNumber(sealType, tag.first)
            updateTagAlpha(sealType, tag.second)
            updateNumTags(sealType, "1")
        }
    }

    private fun ObservationRecord.isGhostNoTagPup(): Boolean {
        if (ageClass != SealAgeClass.PUP.alpha) return false
        val displayedTag = tagIDOne.ifEmpty { tagIDTwo }
        val noRealTag = tagIDOne.isBlank() || tagIDOne == "NoTag"
        val incomplete = sex.isBlank() || tagEvent.isBlank()
        return noRealTag && incomplete && displayedTag.equals("NoTag", ignoreCase = true)
    }
}
