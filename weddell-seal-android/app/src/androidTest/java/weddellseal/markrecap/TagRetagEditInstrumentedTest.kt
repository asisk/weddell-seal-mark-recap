package weddellseal.markrecap

import android.Manifest
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import weddellseal.markrecap.InstrumentedComposeTestSupport.waitForComposeReady
import weddellseal.markrecap.domain.tagretag.data.SEX_CHANGE_ON_EDIT_CONFIRMATION_MESSAGE
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.TagRetagViewModelFactory

/**
 * Device-backed coverage for Parker 2025 recap edit behavior: an edit must update the original
 * Room row (not insert a second copy), a sex change on edit must go through Confirm & Save, and
 * confirming Edit from Recent Observations or Tag/Retag must show one dialog and load that row.
 *
 * Field-commit / blur regressions stay in [TagRetagSaveInstrumentedTest]. JVM tests cover add/remove
 * pup write paths, sex-button visibility in edit, and comment-prefix assembly.
 */
@RunWith(AndroidJUnit4::class)
class TagRetagEditInstrumentedTest {

    companion object {
        private const val TAG_ALPHA = "A"
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

    private fun openDrawer() {
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
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

    private fun navigateToRecentObservations() {
        openDrawer()
        clickDrawerItem("Recent Entries")
        composeRule.onNodeWithText("Recent Observations", substring = true).assertIsDisplayed()
    }

    private fun clearAllObservations() {
        runBlocking {
            app.observationRepo.deleteAll()
        }
    }

    private fun currentObservations(): List<ObservationRecord> = runBlocking {
        app.observationRepo.currentObservationsDescByID.first()
    }

    private fun findUnusedTagNumber(): String {
        for (n in 700..999) {
            val number = n.toString()
            val exists = runBlocking {
                try {
                    app.getWedCheckDao().lookupSealByTagID("$number$TAG_ALPHA")
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (!exists) return number
        }
        error("Could not find an unused 3-digit Tag ID ($TAG_ALPHA) in 700–999")
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

    private fun prefillNewMale(tagRetagViewModel: TagRetagViewModel, tagNumber: String) {
        composeRule.runOnUiThread {
            tagRetagViewModel.prefillSingleMale()
            tagRetagViewModel.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
            tagRetagViewModel.updateTagEventType(
                tagRetagViewModel.primarySeal.value,
                TagEventType.NEW,
            )
            tagRetagViewModel.updateNumTags(SealType.PRIMARY, "1")
            tagRetagViewModel.updateTagAlpha(SealType.PRIMARY, TAG_ALPHA)
            tagRetagViewModel.updateTagNumber(SealType.PRIMARY, tagNumber)
        }
        composeRule.waitForIdle()
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
        try {
            composeRule.onNode(saveMatcher, useUnmergedTree = true).performScrollTo()
        } catch (_: AssertionError) {
            // Already visible or not in a scrollable parent.
        }
        composeRule.waitForIdle()
        try {
            composeRule.onNode(saveMatcher).performSemanticsAction(SemanticsActions.OnClick)
        } catch (_: AssertionError) {
            composeRule.onNode(saveMatcher, useUnmergedTree = true).performClick()
        }
        composeRule.waitForIdle()
    }

    private fun tapConfirmAndSave() {
        val matcher = hasContentDescription("Confirm & Save")
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        try {
            composeRule.onNode(matcher).performSemanticsAction(SemanticsActions.OnClick)
        } catch (_: AssertionError) {
            composeRule.onNode(matcher, useUnmergedTree = true).performClick()
        }
        composeRule.waitForIdle()
    }

    private fun waitForObservation(
        timeoutMillis: Long = 20_000,
        predicate: (ObservationRecord) -> Boolean,
    ): ObservationRecord {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var lastSummaries: List<String> = emptyList()
        while (SystemClock.uptimeMillis() < deadline) {
            val records = currentObservations()
            lastSummaries = records.map { "id=${it.id} ${it.tagIDOne} cond=${it.sealCondition}" }
            records.firstOrNull(predicate)?.let { return it }
            Thread.sleep(250)
        }
        throw AssertionError(
            "No matching observation within ${timeoutMillis}ms. Saved records: $lastSummaries",
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

    private fun dismissSaveSnackbarIfPresent() {
        try {
            composeRule.onNodeWithText("saved!", substring = true, useUnmergedTree = true)
                .performClick()
        } catch (_: AssertionError) {
            // Already dismissed or not shown.
        }
        composeRule.waitForIdle()
    }

    private fun saveNewMaleAndReturnRecord(
        tagRetagViewModel: TagRetagViewModel,
        tagNumber: String,
    ): ObservationRecord {
        prefillNewMale(tagRetagViewModel, tagNumber)
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()
        val tagId = "$tagNumber$TAG_ALPHA"
        val saved = waitForObservation { it.tagIDOne == tagId }
        waitUntilFormReset(tagRetagViewModel)
        return saved
    }

    private fun loadSavedRecordForEdit(
        tagRetagViewModel: TagRetagViewModel,
        saved: ObservationRecord,
    ) {
        composeRule.runOnUiThread {
            tagRetagViewModel.loadSealForEdit(DisplayObservation.Standalone(saved))
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var editMode = false
            composeRule.runOnUiThread {
                editMode = tagRetagViewModel.uiState.value.isEditMode &&
                    tagRetagViewModel.primarySeal.value.observationID == saved.id
            }
            editMode
        }
    }

    @Test
    fun editTagRetag_updatesOriginalRoomRowInsteadOfInsertingACopy() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()
        navigateToTagRetag()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        val tagNumber = findUnusedTagNumber()
        val original = saveNewMaleAndReturnRecord(tagRetagViewModel, tagNumber)

        loadSavedRecordForEdit(tagRetagViewModel, original)
        composeRule.runOnUiThread {
            // Live Home metadata must not overwrite the loaded record's context.
            homeViewModel.updateCensusNumber("99")
            homeViewModel.updateObserversSelection(listOf("ZZZ"))
            tagRetagViewModel.updateCondition(SealType.PRIMARY, SealCondition.FAIR)
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            tagRetagViewModel.hasEdits.value
        }
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        val updated = waitForObservation {
            it.id == original.id && it.sealCondition == SealCondition.FAIR.code
        }
        waitUntilFormReset(tagRetagViewModel)

        val records = currentObservations()
        assertEquals(
            "Edit must update the existing row, not add a second observation",
            1,
            records.size,
        )
        assertEquals(original.id, updated.id)
        assertEquals(original.insertedAt, updated.insertedAt)
        assertNotNull(updated.updatedAt)
        assertEquals(SealCondition.FAIR.code, updated.sealCondition)
        assertEquals(original.date, updated.date)
        assertEquals(original.time, updated.time)
        assertEquals(original.colony, updated.colony)
        assertEquals(original.observerInitials, updated.observerInitials)
        assertEquals(original.censusID, updated.censusID)
        assertEquals(original.latitude, updated.latitude)
        assertEquals(original.longitude, updated.longitude)
        assertTrue(updated.comments.contains("Edited"))
        assertTrue(updated.comments.contains("condition"))
        assertEquals(1, updated.comments.split("Edited").size - 1)
    }

    @Test
    fun editTagRetag_sexChangeRequiresConfirmAndSaveAndKeepsOneRow() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()
        navigateToTagRetag()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        val tagNumber = findUnusedTagNumber()
        val original = saveNewMaleAndReturnRecord(tagRetagViewModel, tagNumber)

        loadSavedRecordForEdit(tagRetagViewModel, original)
        composeRule.runOnUiThread {
            tagRetagViewModel.updateSex(SealType.PRIMARY, SealSex.FEMALE)
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            tagRetagViewModel.hasEdits.value
        }
        waitUntilSaveEnabled(tagRetagViewModel)
        tapSave()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            var needsConfirm = false
            composeRule.runOnUiThread {
                val ui = tagRetagViewModel.uiState.value
                needsConfirm = ui.entryNeedsConfirmation &&
                    ui.validationFailureReason.contains(SEX_CHANGE_ON_EDIT_CONFIRMATION_MESSAGE)
            }
            needsConfirm
        }
        composeRule.onNodeWithText(SEX_CHANGE_ON_EDIT_CONFIRMATION_MESSAGE, substring = true)
            .assertIsDisplayed()
        assertEquals(
            "Save must not write a sex change until Confirm & Save",
            1,
            currentObservations().size,
        )
        assertEquals(SealSex.MALE.alpha, currentObservations().single().sex)

        tapConfirmAndSave()
        val updated = waitForObservation {
            it.id == original.id && it.sex == SealSex.FEMALE.alpha
        }
        waitUntilFormReset(tagRetagViewModel)

        assertEquals(1, currentObservations().size)
        assertEquals(original.id, updated.id)
        assertEquals("", updated.flaggedEntry)
        assertTrue(updated.comments.contains("Edited"))
        assertTrue(updated.comments.contains("sex"))
    }

    @Test
    fun editTagRetag_confirmDialogLoadsTheOriginalRecordOnce() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()
        navigateToTagRetag()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        val tagNumber = findUnusedTagNumber()
        val original = saveNewMaleAndReturnRecord(tagRetagViewModel, tagNumber)
        dismissSaveSnackbarIfPresent()

        composeRule.runOnUiThread {
            tagRetagViewModel.onEditAttempt(DisplayObservation.Standalone(original))
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(
            1,
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )

        composeRule.onNodeWithText("Yes, edit entry", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            var loaded = false
            composeRule.runOnUiThread {
                loaded = tagRetagViewModel.uiState.value.isEditMode &&
                    tagRetagViewModel.primarySeal.value.observationID == original.id
            }
            loaded
        }
        composeRule.onNodeWithText("Tag / Retag", substring = true).assertIsDisplayed()
        assertEquals(
            "Confirming edit on Tag/Retag must not create a second observation",
            1,
            currentObservations().size,
        )
    }

    @Test
    fun recentObservations_editOpensOneDialogAndLoadsTheOriginalRecord() {
        clearAllObservations()
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()
        navigateToTagRetag()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        val tagNumber = findUnusedTagNumber()
        val original = saveNewMaleAndReturnRecord(tagRetagViewModel, tagNumber)

        navigateToRecentObservations()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(original.tagIDOne, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNode(hasContentDescription("More options"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Edit", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(
            "Recent Observations must show a single edit-confirm dialog",
            1,
            composeRule.onAllNodesWithText("Yes, edit entry", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )

        composeRule.onNodeWithText("Yes, edit entry", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            var loaded = false
            composeRule.runOnUiThread {
                loaded = tagRetagViewModel.uiState.value.isEditMode &&
                    tagRetagViewModel.primarySeal.value.observationID == original.id
            }
            loaded
        }
        composeRule.onNodeWithText("Tag / Retag", substring = true).assertIsDisplayed()
        assertEquals(1, currentObservations().size)
        assertEquals(original.id, currentObservations().single().id)
    }
}
