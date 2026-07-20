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
}
