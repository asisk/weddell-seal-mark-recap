package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.TagRetagViewModelFactory

/**
 * End-to-end regression for pending Tag ID edits: user commits tag number 456 via the UI,
 * re-types 789 in the Tag ID field, and taps Save without blurring the field. The saved
 * observation must contain 789A (alpha is preset; this test targets pending number commits).
 *
 * Uses public UI text/semantics only (no production test hooks). Non-tag required fields are
 * set on [TagRetagViewModel]; tag entry uses the real [TagIDOutlinedTextField].
 */
@RunWith(AndroidJUnit4::class)
class TagRetagSaveInstrumentedTest {

    companion object {
        /** Primary seal tag row label when tag event is not Retag. */
        private const val TAG_ID_LABEL = "Tag ID"
        /** [TagIDOutlinedTextField] label / placeholder (public UI strings). */
        private const val TAG_NUMBER_LABEL = "3 or 4 Digit Tag Number"
        private const val TAG_NUMBER_PLACEHOLDER = "Enter Tag Number"
        /** UI only offers A, C, D. */
        private const val TAG_ALPHA = "A"
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private val app: ObservationLogApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication

    /**
     * Tag number [OutlinedTextField]. The "Tag ID" label is a sibling, not an ancestor, so match
     * on the field's own label or placeholder from [TagIDOutlinedTextField].
     */
    private val primaryTagNumberFieldMatcher: SemanticsMatcher
        get() = hasSetTextAction().and(
            hasText(TAG_NUMBER_LABEL, substring = true)
                .or(hasText(TAG_NUMBER_PLACEHOLDER, substring = true)),
        )

    private fun openDrawer() {
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

    private fun clearAllObservations() {
        runBlocking {
            app.observationRepo.deleteAll()
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
    }

    private fun prefillSealExceptTag(tagRetagViewModel: TagRetagViewModel) {
        composeRule.runOnUiThread {
            tagRetagViewModel.prefillSingleMale()
            tagRetagViewModel.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
            tagRetagViewModel.updateTagEventType(
                tagRetagViewModel.primarySeal.value,
                TagEventType.NEW,
            )
            tagRetagViewModel.updateNumTags(SealType.PRIMARY, "1")
            // Alpha buttons are not reliably exposed to UI tests on device; this test targets
            // pending tag *number* commits on save, not alpha selection.
            tagRetagViewModel.updateTagAlpha(SealType.PRIMARY, TAG_ALPHA)
        }
        composeRule.waitForIdle()
    }

    private fun scrollToTagIdSection() {
        composeRule.onNodeWithText(TAG_ID_LABEL, useUnmergedTree = true).performScrollTo()
        composeRule.waitForIdle()
    }

    private fun waitUntilTagIdSectionReady() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(TAG_ID_LABEL, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun primaryTagNumberField() =
        if (composeRule.onAllNodes(primaryTagNumberFieldMatcher, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            composeRule.onNode(primaryTagNumberFieldMatcher, useUnmergedTree = true)
        } else {
            // Label/placeholder can be on a separate semantics node from the editable text.
            // On the primary seal card the tag field is the first editable above the comment box.
            composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[0]
        }

    /** Commits the tag number by clearing focus (same as tapping outside the field). */
    private fun commitTagNumberField() {
        composeRule.onNodeWithText(TAG_ID_LABEL, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
    }

    /** Types [number] and commits it by blurring the field. */
    private fun enterAndCommitTagNumber(number: String) {
        scrollToTagIdSection()
        waitUntilTagIdSectionReady()
        val field = primaryTagNumberField()
        field.performClick()
        field.performTextReplacement(number)
        commitTagNumberField()
    }

    private fun replaceTagNumberWithoutBlur(number: String) {
        scrollToTagIdSection()
        val field = primaryTagNumberField()
        field.performClick()
        field.performTextReplacement(number)
        composeRule.waitForIdle()
    }

    private fun tapSave() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasContentDescription("Save Seal"), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        try {
            composeRule.onNodeWithText("# of Tags", useUnmergedTree = true).performScrollTo()
            composeRule.waitForIdle()
        } catch (_: AssertionError) {
            // Footer may already be visible on tall layouts.
        }
        composeRule.onNode(hasContentDescription("Save Seal"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    private fun waitForObservation(
        timeoutMillis: Long = 15_000,
        predicate: (ObservationRecord) -> Boolean,
    ): ObservationRecord {
        var found: ObservationRecord? = null
        composeRule.waitUntil(timeoutMillis) {
            val match = runBlocking {
                app.observationRepo.currentObservationsDescByID.first()
                    .firstOrNull(predicate)
            }
            if (match != null) {
                found = match
                true
            } else {
                false
            }
        }
        return found!!
    }

    @Test
    fun saveTagRetag_persistsInProgressTagIdWithoutRequiringBlur() {
        clearAllObservations()
        setupTestObservers()
        navigateToTagRetag()

        prefillSealExceptTag(getTagRetagViewModel(getHomeViewModel()))

        enterAndCommitTagNumber("456")
        replaceTagNumberWithoutBlur("789")
        tapSave()

        val expectedTagId = "789$TAG_ALPHA"
        val saved = waitForObservation { it.tagIDOne == expectedTagId }
        assertEquals(
            "Save should persist the in-progress Tag ID edit even when the field still has focus",
            expectedTagId,
            saved.tagIDOne,
        )
        assertEquals(TagEventType.NEW.alpha, saved.tagEvent)
    }
}
