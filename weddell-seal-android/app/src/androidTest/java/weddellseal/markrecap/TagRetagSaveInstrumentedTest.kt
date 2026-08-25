package weddellseal.markrecap

import android.Manifest
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import weddellseal.markrecap.InstrumentedComposeTestSupport.waitForComposeReady
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.COMMENTS_FIELD_TEST_TAG
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.TagRetagViewModelFactory

/**
 * Regression for pending field edits on save (Tag ID and Comments): the user types in a focused
 * field and taps Save without blurring. The saved observation must include the in-progress text,
 * and the next blank entry must not inherit it (including when a second seal is then saved).
 *
 * Blur/commit behavior is covered by JVM tests ([TagRetagViewModelTest]). This suite uses the
 * ViewModel only for non-field setup and the initial committed tag number, then drives the
 * in-progress edit and Save through public UI semantics.
 */
@RunWith(AndroidJUnit4::class)
class TagRetagSaveInstrumentedTest {

    companion object {
        private const val TAG_ID_LABEL = "Tag ID"
        private const val TAG_NUMBER_LABEL = "3 or 4 Digit Tag Number"
        private const val TAG_NUMBER_PLACEHOLDER = "Enter Tag Number"
        private const val TAG_ALPHA = "A"
        /**
         * 4-digit NEW tags for comment tests. Auto-committed at 4 digits (live lookup); that is
         * fine here because the regression is the comment field, not pending tag numbers.
         * Must not collide with 3-digit tags used by the save-without-blur Tag ID tests.
         */
        private const val COMMENT_TEST_TAG_NUMBER = "9911"
        private const val SECOND_SEAL_TAG_NUMBER = "9912"
        /** High speno unlikely to collide with a real census row on a field tablet. */
        private const val MARKED_IN_PROGRESS_SPENO = 39992
        private const val MARKED_COMMITTED_SPENO = 39991
        private const val TISSUE_LABEL = "Tissue"
        private const val IN_PROGRESS_COMMENT = "scar on left flipper"
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @get:Rule
    val testRules: TestRule = InstrumentedComposeTestSupport.ruleChain(
        grantPermissionRule,
        composeRule,
    )

    private val app: ObservationLogApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication

    private fun tagNumberFieldMatcher(displayedNumber: String): SemanticsMatcher =
        hasSetTextAction().and(hasText(displayedNumber))

    private fun tagNumberFieldMatcherFallback(): SemanticsMatcher =
        hasSetTextAction().and(
            hasText(TAG_NUMBER_LABEL, substring = true)
                .or(hasText(TAG_NUMBER_PLACEHOLDER, substring = true)),
        )

    private fun openDrawer() {
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    /** Scrolls [matcher] into view when it lives inside a scrollable parent. */
    private fun scrollIntoView(matcher: SemanticsMatcher) {
        try {
            composeRule.onNode(matcher, useUnmergedTree = true).performScrollTo()
        } catch (_: AssertionError) {
            // Already visible or not in a scrollable parent.
        }
        composeRule.waitForIdle()
    }

    private fun clickDrawerItem(text: String) {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            // Drawer item may not be in a scrollable parent.
        }
        node.performClick()
        composeRule.waitForIdle()
    }

    private fun navigateToTagRetag() {
        openDrawer()
        clickDrawerItem("Tag/Retag")
        composeRule.onNodeWithText("Tag / Retag", substring = true).assertIsDisplayed()
    }

    private fun clearAllObservations() {
        runBlocking {
            app.observationRepo.deleteAll()
        }
    }

    /**
     * 3-digit Tag IDs that are not in WedCheck. Required so:
     * - NEW save does not take the "tag already used" confirmation path (empty records).
     * - MARKED lookup `LIMIT 1` cannot return a leftover census row for 456A/789A.
     * 3-digit on purpose: 4-digit input auto-commits while typing, which would not exercise
     * the pending-tag Save path.
     */
    private fun findUnusedTagNumbers(count: Int): List<String> {
        val unused = mutableListOf<String>()
        for (n in 700..999) {
            if (unused.size >= count) break
            val number = n.toString()
            if (!tagExistsInWedCheck("$number$TAG_ALPHA")) {
                unused.add(number)
            }
        }
        check(unused.size == count) {
            "Could not find $count unused 3-digit Tag IDs (${TAG_ALPHA}) in 700–999"
        }
        return unused
    }

    private fun tagExistsInWedCheck(tagId: String): Boolean = runBlocking {
        try {
            app.getWedCheckDao().lookupSealByTagID(tagId)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Seeds WedCheck rows for the Marked save-without-blur path (fix #6).
     * The in-progress tag must resolve to [inProgressSpeno] at persistence time.
     */
    private fun seedWedCheckForMarkedSave(
        committedNumber: String,
        inProgressNumber: String,
        inProgressSpeno: Int = MARKED_IN_PROGRESS_SPENO,
    ) {
        runBlocking {
            val fileUploadId = app.getFileUploadDao().insertFileUpload(
                FileUploadEntity(
                    fileType = FileType.WEDCHECK,
                    fileAction = FileAction.UPLOAD.name,
                    filename = "instrumented-wedcheck.csv",
                    status = FileStatus.IDLE,
                    statusMessage = null,
                    recordCount = 0,
                ),
            )
            val season = getCurrentYear()
            fun wedCheckRow(speno: Int, tagId: String) = WedCheckRecord(
                speno = speno,
                season = season,
                ageClass = "A",
                sex = "M",
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
                fileUploadId = fileUploadId,
                latitude = -77.0,
                longitude = 166.0,
            )
            app.getWedCheckDao().insertWedCheckRecord(
                wedCheckRow(MARKED_COMMITTED_SPENO, "$committedNumber$TAG_ALPHA"),
            )
            app.getWedCheckDao().insertWedCheckRecord(
                wedCheckRow(inProgressSpeno, "$inProgressNumber$TAG_ALPHA"),
            )
        }
    }

    private fun getHomeViewModel(): HomeViewModel {
        val activity = composeRule.activity
        val factory = HomeViewModelFactory(
            FusedLocationSource(activity),
            SealColonyRepository(app.getSealColoniesDao()),
            ObserversRepository(app.getObserversDao()),
        )
        return ViewModelProvider(activity, factory)[HomeViewModel::class.java]
    }

    private fun getTagRetagViewModel(homeViewModel: HomeViewModel): TagRetagViewModel {
        val factory = TagRetagViewModelFactory(
            app,
            app.observationRepo,
            WedCheckRepository(app.getWedCheckDao(), app.getFileUploadDao()),
            homeViewModel.metadata,
            homeViewModel.uiState,
        )
        return ViewModelProvider(composeRule.activity, factory)[TagRetagViewModel::class.java]
    }

    private fun setupTestObservers() {
        val homeViewModel = getHomeViewModel()
        composeRule.runOnUiThread {
            homeViewModel.updateObserversSelection(listOf("TST"))
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            var valid = false
            composeRule.runOnUiThread {
                valid = homeViewModel.metadata.value.isValid
            }
            valid
        }
    }

    private fun prefillSealAndCommitTag(
        tagRetagViewModel: TagRetagViewModel,
        tagEventType: TagEventType = TagEventType.NEW,
        tagNumber: String,
    ) {
        composeRule.runOnUiThread {
            tagRetagViewModel.prefillSingleMale()
            tagRetagViewModel.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
            tagRetagViewModel.updateTagEventType(
                tagRetagViewModel.primarySeal.value,
                tagEventType,
            )
            tagRetagViewModel.updateNumTags(SealType.PRIMARY, "1")
            tagRetagViewModel.updateTagAlpha(SealType.PRIMARY, TAG_ALPHA)
            tagRetagViewModel.updateTagNumber(SealType.PRIMARY, tagNumber)
        }
        composeRule.waitForIdle()
    }

    private fun scrollToTagIdSection() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(TAG_ID_LABEL, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        scrollIntoView(hasText(TAG_ID_LABEL, substring = true))
    }

    private fun waitUntilTagNumberFieldShows(number: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(tagNumberFieldMatcher(number), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeRule.onAllNodes(tagNumberFieldMatcherFallback(), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }

    private fun tagNumberFieldMatcherResolved(displayedNumber: String): SemanticsMatcher =
        if (composeRule.onAllNodes(
                tagNumberFieldMatcher(displayedNumber),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        ) {
            tagNumberFieldMatcher(displayedNumber)
        } else {
            tagNumberFieldMatcherFallback()
        }

    /**
     * Re-fetch the matcher on each attempt. [performTextReplacement] focuses the field and can
     * trigger recomposition; avoid [performClick] first, which was dropping the semantics node on
     * API 36 before text could be entered.
     */
    private fun replaceTextWithRetry(matcher: SemanticsMatcher, number: String, attempts: Int = 5) {
        var lastError: AssertionError? = null
        repeat(attempts) {
            composeRule.waitForIdle()
            try {
                composeRule.onNode(matcher, useUnmergedTree = true).performTextReplacement(number)
                composeRule.waitForIdle()
                return
            } catch (error: AssertionError) {
                lastError = error
                Thread.sleep(150)
            }
        }
        throw lastError!!
    }

    private fun replaceTagNumberWithoutBlur(committedNumber: String, inProgressNumber: String) {
        scrollToTagIdSection()
        waitUntilTagNumberFieldShows(committedNumber)
        composeRule.waitForIdle()
        replaceTextWithRetry(tagNumberFieldMatcherResolved(committedNumber), inProgressNumber)
        waitUntilTagNumberFieldShows(inProgressNumber)
    }

    private fun commentFieldMatcher(): SemanticsMatcher =
        hasTestTag(COMMENTS_FIELD_TEST_TAG)

    private fun commentFieldMatcherWithText(text: String): SemanticsMatcher =
        hasTestTag(COMMENTS_FIELD_TEST_TAG).and(hasText(text))

    private fun scrollToCommentSection() {
        // Tissue sits beside Comments near the bottom of the seal card; scroll it into view first.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(TISSUE_LABEL, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeRule.onAllNodes(commentFieldMatcher(), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        scrollIntoView(hasText(TISSUE_LABEL, substring = true))
        scrollIntoView(commentFieldMatcher())
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(commentFieldMatcher(), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Types a comment while leaving the field focused (no Done / clearFocus before Save).
     * Mirrors [replaceTagNumberWithoutBlur] for the Comments regression.
     */
    private fun enterCommentWithoutBlur(comment: String) {
        scrollToCommentSection()
        composeRule.waitForIdle()
        replaceTextWithRetry(commentFieldMatcher(), comment)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(commentFieldMatcherWithText(comment), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun assertCommentNotVisible(comment: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(commentFieldMatcherWithText(comment), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private fun waitUntilSaveEnabled(tagRetagViewModel: TagRetagViewModel) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            var enabled = false
            composeRule.runOnUiThread {
                enabled = tagRetagViewModel.uiState.value.isSaveEnabled
            }
            enabled
        }
    }

    private fun tapSave() {
        val saveMatcher = hasContentDescription("Save Seal")
        scrollIntoView(saveMatcher)
        composeRule.waitForIdle()
        // Invoke the FAB onClick via semantics so a covering IME cannot swallow a coordinate click.
        try {
            composeRule.onNode(saveMatcher).performSemanticsAction(SemanticsActions.OnClick)
        } catch (_: AssertionError) {
            composeRule.onNode(saveMatcher, useUnmergedTree = true).performClick()
        }
        composeRule.waitForIdle()
    }

    private fun saveDiagnostic(tagRetagViewModel: TagRetagViewModel): String {
        var text = ""
        composeRule.runOnUiThread {
            val ui = tagRetagViewModel.uiState.value
            val seal = tagRetagViewModel.primarySeal.value
            text = "confirmation=${ui.entryNeedsConfirmation} " +
                "saveAttempted=${ui.isSaveAttempted} " +
                "saveEnabled=${ui.isSaveEnabled} " +
                "tag=${seal.tagNumber}${seal.tagAlpha} " +
                "errors=${seal.validationErrors}"
        }
        return text
    }

    private fun waitForObservation(
        timeoutMillis: Long = 20_000,
        diagnostic: () -> String = { "" },
        predicate: (ObservationRecord) -> Boolean,
    ): ObservationRecord {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var lastSummaries: List<String> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            val records = runBlocking {
                app.observationRepo.currentObservationsDescByID.first()
            }
            lastSummaries = records.map { "${it.tagIDOne} comments=${it.comments}" }
            records.firstOrNull(predicate)?.let { return it }
            Thread.sleep(250)
        }
        val extra = diagnostic().ifBlank { "" }
        throw AssertionError(
            "No matching observation within ${timeoutMillis}ms. " +
                "Saved records: $lastSummaries" +
                if (extra.isNotEmpty()) ". $extra" else "",
        )
    }

    private fun waitUntilFormReset(tagRetagViewModel: TagRetagViewModel) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            var reset = false
            composeRule.runOnUiThread {
                val seal = tagRetagViewModel.primarySeal.value
                reset = !seal.isEntryStarted && seal.tagNumber.isEmpty()
            }
            reset
        }
        composeRule.waitForIdle()
    }

    private fun assertTagNumberNotVisible(number: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(tagNumberFieldMatcher(number), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private fun assertSpenoNotDisplayed(speno: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(
                hasText("Speno: $speno", substring = true),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun saveTagRetag_persistsInProgressTagIdWithoutRequiringBlur() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        val (committedNumber, inProgressNumber) = findUnusedTagNumbers(2)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        prefillSealAndCommitTag(tagRetagViewModel, tagNumber = committedNumber)
        navigateToTagRetag()

        replaceTagNumberWithoutBlur(committedNumber, inProgressNumber)
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        val expectedTagId = "$inProgressNumber$TAG_ALPHA"
        val saved = waitForObservation(
            diagnostic = { saveDiagnostic(tagRetagViewModel) },
        ) { it.tagIDOne == expectedTagId }
        assertEquals(
            "Save should persist the in-progress Tag ID edit even when the field still has focus",
            expectedTagId,
            saved.tagIDOne,
        )
        assertEquals(TagEventType.NEW.alpha, saved.tagEvent)
        assertTrue(saved.observerInitials.contains("TST"))
    }

    /**
     * Fix #6: Marked save without blur must persist both the committed in-progress tag and speno
     * from WedCheck (PML scenario: retag ID edited, save while tag field still focused).
     */
    @Test
    fun saveTagRetag_persistsSpenoForMarkedSealWithoutRequiringBlur() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        val (committedNumber, inProgressNumber) = findUnusedTagNumbers(2)
        seedWedCheckForMarkedSave(committedNumber, inProgressNumber)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        prefillSealAndCommitTag(
            tagRetagViewModel,
            TagEventType.MARKED,
            tagNumber = committedNumber,
        )
        navigateToTagRetag()

        replaceTagNumberWithoutBlur(committedNumber, inProgressNumber)
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        val expectedTagId = "$inProgressNumber$TAG_ALPHA"
        val saved = waitForObservation(
            diagnostic = { saveDiagnostic(tagRetagViewModel) },
        ) { it.tagIDOne == expectedTagId }
        assertEquals(
            "Save should persist the in-progress Tag ID edit even when the field still has focus",
            expectedTagId,
            saved.tagIDOne,
        )
        assertEquals(TagEventType.MARKED.alpha, saved.tagEvent)
        assertEquals(
            MARKED_IN_PROGRESS_SPENO.toString(),
            saved.speno,
        )
        assertNotEquals("0", saved.speno)
        assertTrue(saved.observerInitials.contains("TST"))
    }

    /**
     * Fix #3 / #6: after save with the tag field still focused, the next entry must not show the
     * previous tag number or WedCheck speno (PML "leftover tag/speno on next entry").
     */
    @Test
    fun saveTagRetag_clearsTagFieldForNextEntryAfterSaveWithoutBlur() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        val (committedNumber, inProgressNumber) = findUnusedTagNumbers(2)
        seedWedCheckForMarkedSave(committedNumber, inProgressNumber)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        prefillSealAndCommitTag(
            tagRetagViewModel,
            TagEventType.MARKED,
            tagNumber = committedNumber,
        )
        navigateToTagRetag()

        replaceTagNumberWithoutBlur(committedNumber, inProgressNumber)
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        val expectedTagId = "$inProgressNumber$TAG_ALPHA"
        waitForObservation(
            diagnostic = { saveDiagnostic(tagRetagViewModel) },
        ) { it.tagIDOne == expectedTagId }
        waitUntilFormReset(tagRetagViewModel)

        scrollToTagIdSection()
        assertTagNumberNotVisible(inProgressNumber)
        assertTagNumberNotVisible(committedNumber)
        assertSpenoNotDisplayed(MARKED_IN_PROGRESS_SPENO)

        composeRule.runOnUiThread {
            val seal = tagRetagViewModel.primarySeal.value
            assertFalse("Next entry should start with a blank seal", seal.isEntryStarted)
            assertEquals("", seal.tagNumber)
            assertFalse(seal.hasWedCheckMatch)
        }
    }

    /**
     * Reported bug: type a comment on seal 1 and Save without leaving the field; the note was
     * missing from seal 1 and appeared when entering / saving seal 2.
     *
     * Proves both sides: seal 1 persists the in-progress comment, and seal 2 is saved without it.
     */
    @Test
    fun saveTagRetag_commentFromFirstSealDoesNotPersistOnSecondSeal() {
        clearAllObservations()
        // Reach a stable hierarchy before ViewModel setup; cold starts on tablets can exceed 20s.
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)

        // --- Seal 1: comment typed, Save while Comments still focused ---
        navigateToTagRetag()
        prefillSealAndCommitTag(tagRetagViewModel, tagNumber = COMMENT_TEST_TAG_NUMBER)
        composeRule.waitForIdle()
        enterCommentWithoutBlur(IN_PROGRESS_COMMENT)

        composeRule.runOnUiThread {
            assertEquals(
                "Comment must still be pending (not committed via blur) before Save",
                "",
                tagRetagViewModel.primarySeal.value.comment,
            )
        }

        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        val firstTagId = "$COMMENT_TEST_TAG_NUMBER$TAG_ALPHA"
        val firstSaved = waitForObservation {
            it.tagIDOne == firstTagId && it.comments.contains(IN_PROGRESS_COMMENT)
        }
        assertEquals(TagEventType.NEW.alpha, firstSaved.tagEvent)
        waitUntilFormReset(tagRetagViewModel)

        scrollToCommentSection()
        assertCommentNotVisible(IN_PROGRESS_COMMENT)
        composeRule.runOnUiThread {
            assertEquals(
                "Blank next entry must not inherit the previous seal's comment",
                "",
                tagRetagViewModel.primarySeal.value.comment,
            )
        }

        // --- Seal 2: new entry, no comment typed, then Save ---
        // Drive save through the ViewModel: the seal-1 success snackbar can cover the Save FAB
        // and does not reliably dismiss under instrumentation. The regression under test is that
        // seal 1's in-progress comment must not be on the next seal's model when it is saved.
        prefillSealAndCommitTag(tagRetagViewModel, tagNumber = SECOND_SEAL_TAG_NUMBER)
        composeRule.waitForIdle()

        scrollToCommentSection()
        assertCommentNotVisible(IN_PROGRESS_COMMENT)
        composeRule.runOnUiThread {
            assertEquals("", tagRetagViewModel.primarySeal.value.comment)
            assertEquals(SECOND_SEAL_TAG_NUMBER, tagRetagViewModel.primarySeal.value.tagNumber)
        }

        waitUntilSaveEnabled(tagRetagViewModel)
        composeRule.runOnUiThread {
            tagRetagViewModel.attemptSave(homeViewModel.getColonyLocation())
        }
        composeRule.waitForIdle()

        val secondTagId = "$SECOND_SEAL_TAG_NUMBER$TAG_ALPHA"
        val secondSaved = waitForObservation { it.tagIDOne == secondTagId }
        assertFalse(
            "Comment from the first seal must not be saved on the next seal. " +
                "Second seal comments: ${secondSaved.comments}",
            secondSaved.comments.contains(IN_PROGRESS_COMMENT),
        )

        // Seal 1 record must still own the comment (not moved/cleared by the second save).
        val records = runBlocking {
            app.observationRepo.currentObservationsDescByID.first()
        }
        val firstAfterSecondSave = records.first { it.tagIDOne == firstTagId }
        assertTrue(firstAfterSecondSave.comments.contains(IN_PROGRESS_COMMENT))
    }
}
