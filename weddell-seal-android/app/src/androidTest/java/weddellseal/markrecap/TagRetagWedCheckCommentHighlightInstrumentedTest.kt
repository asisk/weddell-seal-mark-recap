package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.FieldHighlight
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.TagRetagViewModelFactory

/**
 * On-device coverage for Parker 2025 recap highlights: a WedCheck comment on Tag/Retag uses
 * [FieldHighlight] (black background, white text). JVM [TabbedCardsTest] mounts the chip
 * directly; this seeds WedCheck and waits for the live lookup on the enter screen.
 */
@RunWith(AndroidJUnit4::class)
class TagRetagWedCheckCommentHighlightInstrumentedTest {

    companion object {
        private const val TAG_ALPHA = "C"
        private const val COMMENT_TAG_NUMBER = "9884"
        private const val COMMENT_TAG_ID = "$COMMENT_TAG_NUMBER$TAG_ALPHA"
        private const val COMMENT_SPENO = 39982
        private const val WEDCHECK_COMMENT = "instrumented wedcheck notes"
        private const val BLANK_TAG_NUMBER = "9885"
        private const val BLANK_TAG_ID = "$BLANK_TAG_NUMBER$TAG_ALPHA"
        private const val BLANK_SPENO = 39983
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
    fun markedSealWithWedCheckComment_isHighlighted() {
        seedWedCheckSeal(COMMENT_SPENO, COMMENT_TAG_ID, WEDCHECK_COMMENT)
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        prefillMarkedTag(tagRetagViewModel, COMMENT_TAG_NUMBER)
        navigateToTagRetag()

        waitUntilSpenoDisplayed(tagRetagViewModel, COMMENT_SPENO)
        waitUntilInTree(WEDCHECK_COMMENT)
        waitUntilHighlightCount(1)
    }

    @Test
    fun markedSealWithBlankWedCheckComment_isNotHighlighted() {
        seedWedCheckSeal(BLANK_SPENO, BLANK_TAG_ID, "   ")
        composeRule.waitForComposeReady(timeoutMillis = 45_000)
        setupTestObservers()

        val homeViewModel = getHomeViewModel()
        val tagRetagViewModel = getTagRetagViewModel(homeViewModel)
        prefillMarkedTag(tagRetagViewModel, BLANK_TAG_NUMBER)
        navigateToTagRetag()

        waitUntilSpenoDisplayed(tagRetagViewModel, BLANK_SPENO)
        waitUntilHighlightCount(0)
    }

    private fun seedWedCheckSeal(speno: Int, tagId: String, comments: String) {
        runBlocking {
            val fileUploadId = app.getFileUploadDao().insertFileUpload(
                FileUploadEntity(
                    fileType = FileType.WEDCHECK,
                    fileAction = FileAction.UPLOAD.name,
                    filename = "instrumented-wedcheck-comment-highlight.csv",
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
                    tagIdTwo = "NA",
                    comments = comments,
                    ageYears = 3,
                    tissueSampled = "NA",
                    pupinMassStudy = "NA",
                    numPreviousPups = "NA",
                    pupinTTStudy = "NA",
                    momMassMeasurements = "NA",
                    condition = SealCondition.GOOD.code,
                    lastPhysio = "NA",
                    population = "NA",
                    fileUploadId = fileUploadId,
                    latitude = -77.0,
                    longitude = 166.0,
                ),
            )
            val seeded = app.getWedCheckDao().lookupSealByTagID(tagId)
            assertEquals(speno, seeded.speno)
        }
    }

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

    private fun prefillMarkedTag(tagRetagViewModel: TagRetagViewModel, tagNumber: String) {
        composeRule.runOnUiThread {
            tagRetagViewModel.prefillSingleMale()
            tagRetagViewModel.updateCondition(SealType.PRIMARY, SealCondition.GOOD)
            tagRetagViewModel.updateTagEventType(
                tagRetagViewModel.primarySeal.value,
                TagEventType.MARKED,
            )
            tagRetagViewModel.updateNumTags(SealType.PRIMARY, "1")
            tagRetagViewModel.updateTagAlpha(SealType.PRIMARY, TAG_ALPHA)
            tagRetagViewModel.updateTagNumber(SealType.PRIMARY, tagNumber)
        }
        composeRule.waitForIdle()
    }

    private fun waitUntilSpenoDisplayed(tagRetagViewModel: TagRetagViewModel, speno: Int) {
        val text = "Speno: $speno"
        val foundInTree = try {
            composeRule.waitUntil(timeoutMillis = 8_000) {
                composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            true
        } catch (_: ComposeTimeoutException) {
            false
        }
        if (!foundInTree) {
            composeRule.runOnUiThread {
                tagRetagViewModel.requestCurrentWedCheckMatch(tagRetagViewModel.primarySeal.value)
            }
            composeRule.waitForIdle()
        }
        waitUntilInTree(text)
    }

    private fun waitUntilInTree(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
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
