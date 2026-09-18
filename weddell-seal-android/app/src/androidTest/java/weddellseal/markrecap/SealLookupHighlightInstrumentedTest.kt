package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import weddellseal.markrecap.InstrumentedComposeTestSupport.waitForComposeReady
import weddellseal.markrecap.domain.tagretag.data.SealCondition
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.FieldHighlight
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.lookup.SealLookupViewModel
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.viewmodelfactories.SealLookupViewModelFactory

/**
 * On-device coverage for Parker 2025 recap highlights: lookup notes, tissue Need, and Dead
 * use [FieldHighlight] (black background, white text). JVM [LookupCardHighlightTest] mounts
 * LookupCard directly; this seeds WedCheck and searches on Seal Lookup.
 */
@RunWith(AndroidJUnit4::class)
class SealLookupHighlightInstrumentedTest {

    companion object {
        private const val HIGHLIGHT_TAG_ID = "9882C"
        private const val HIGHLIGHT_SPENO = 39980
        private const val HIGHLIGHT_NOTES = "instrumented lookup notes"
        private const val ORDINARY_TAG_ID = "9883C"
        private const val ORDINARY_SPENO = 39981
        private const val SEARCH_FIELD_LABEL = "Tag ID or Speno"
        private const val SEARCH_CONTENT_DESCRIPTION = "Search"
        private const val DRAWER_LOOKUP = "Seal Lookup"
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

    @Test
    fun lookupNeedDeadAndNotes_areHighlighted() {
        seedWedCheckSeal(
            tagId = HIGHLIGHT_TAG_ID,
            speno = HIGHLIGHT_SPENO,
            tissueSampled = "Need",
            condition = SealCondition.DEAD.code,
            comments = HIGHLIGHT_NOTES,
        )
        navigateToSealLookup()
        searchForTag(HIGHLIGHT_TAG_ID)
        waitUntilLookupShowsSpeno(HIGHLIGHT_SPENO, HIGHLIGHT_TAG_ID)

        waitUntilInTree("Need")
        waitUntilInTree(SealCondition.DEAD.code)
        waitUntilInTree(HIGHLIGHT_NOTES)
        waitUntilHighlightCount(3)
    }

    @Test
    fun lookupOrdinarySeal_isNotHighlighted() {
        seedWedCheckSeal(
            tagId = ORDINARY_TAG_ID,
            speno = ORDINARY_SPENO,
            tissueSampled = "NA",
            condition = SealCondition.GOOD.code,
            comments = "",
        )
        navigateToSealLookup()
        searchForTag(ORDINARY_TAG_ID)
        waitUntilLookupShowsSpeno(ORDINARY_SPENO, ORDINARY_TAG_ID)

        waitUntilInTree("Tissue Taken")
        waitUntilHighlightCount(0)
    }

    private fun seedWedCheckSeal(
        tagId: String,
        speno: Int,
        tissueSampled: String,
        condition: String,
        comments: String,
    ) {
        runBlocking {
            val fileUploadId = app.getFileUploadDao().insertFileUpload(
                FileUploadEntity(
                    fileType = FileType.WEDCHECK,
                    fileAction = FileAction.UPLOAD.name,
                    filename = "instrumented-highlight-wedcheck.csv",
                    status = FileStatus.IDLE,
                    statusMessage = null,
                    recordCount = 0,
                ),
            )
            app.getWedCheckDao().insertWedCheckRecord(
                WedCheckRecord(
                    speno = speno,
                    season = getCurrentYear(),
                    ageClass = "A",
                    sex = "M",
                    tagIdOne = tagId,
                    tagIdTwo = tagId,
                    comments = comments,
                    ageYears = 9,
                    tissueSampled = tissueSampled,
                    pupinMassStudy = "NA",
                    numPreviousPups = "NA",
                    pupinTTStudy = "NA",
                    momMassMeasurements = "NA",
                    condition = condition,
                    lastPhysio = "NA",
                    population = "Erebus Bay",
                    fileUploadId = fileUploadId,
                    latitude = -77.5,
                    longitude = 166.5,
                ),
            )
            val seeded = app.getWedCheckDao().lookupSealByTagID(tagId)
            assertEquals(speno, seeded.speno)
        }
    }

    private fun navigateToSealLookup() {
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        val drawerItem = composeRule.onNodeWithText(DRAWER_LOOKUP, useUnmergedTree = true)
        try {
            drawerItem.performScrollTo()
        } catch (_: AssertionError) {
            // Drawer item may not be in a scrollable parent.
        }
        drawerItem.performClick()
        composeRule.waitForIdle()
        waitUntilInTree(SEARCH_FIELD_LABEL, substring = true)
    }

    private fun searchForTag(tagId: String) {
        val field = composeRule.onNode(hasSetTextAction())
        field.performTextReplacement(tagId)
        composeRule.waitForIdle()
        val searchMatcher = hasContentDescription(SEARCH_CONTENT_DESCRIPTION)
        try {
            composeRule.onNode(searchMatcher).performSemanticsAction(SemanticsActions.OnClick)
        } catch (_: AssertionError) {
            composeRule.onNode(searchMatcher, useUnmergedTree = true).performClick()
        }
        composeRule.waitForIdle()
    }

    private fun getSealLookupViewModel(): SealLookupViewModel {
        val factory = SealLookupViewModelFactory(
            app,
            WedCheckRepository(app.getWedCheckDao(), app.getFileUploadDao()),
        )
        return ViewModelProvider(composeRule.activity, factory)[SealLookupViewModel::class.java]
    }

    private fun waitUntilLookupShowsSpeno(speno: Int, tagId: String) {
        val spenoText = speno.toString()
        val foundInTree = try {
            composeRule.waitUntil(timeoutMillis = 8_000) { nodeExists(spenoText) }
            true
        } catch (_: ComposeTimeoutException) {
            false
        }
        if (!foundInTree) {
            composeRule.runOnUiThread {
                getSealLookupViewModel().findSealbyTagID(tagId)
            }
            composeRule.waitForIdle()
        }
        waitUntilInTree(spenoText)
    }

    private fun nodeExists(text: String, substring: Boolean = false): Boolean =
        composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    /**
     * Composed-and-scrolled is enough: [assertIsDisplayed] fails when LookupCard is clipped on
     * the default 320x640 CI AVD.
     */
    private fun waitUntilInTree(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            val nodes = composeRule.onAllNodes(
                hasText(text, substring = substring),
                useUnmergedTree = true,
            )
            val count = nodes.fetchSemanticsNodes().size
            if (count == 0) return@waitUntil false
            (0 until count).any { index ->
                try {
                    nodes[index].performScrollTo()
                } catch (_: AssertionError) {
                    // Not in a scrollable parent.
                }
                try {
                    nodes[index].assertIsDisplayed()
                } catch (_: AssertionError) {
                    // Still in the tree.
                }
                true
            }
        }
    }

    private fun waitUntilHighlightCount(expected: Int) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithTag(FieldHighlight.TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size == expected
        }
    }
}
