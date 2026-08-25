package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.frameworks.google.fusedLocation.FusedLocationSource
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.frameworks.room.observers.ObserversRepository
import weddellseal.markrecap.frameworks.room.sealColonies.SealColony
import weddellseal.markrecap.frameworks.room.sealColonies.SealColonyRepository
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRecord
import weddellseal.markrecap.frameworks.room.wedCheck.WedCheckRepository
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.home.HomeViewModel
import weddellseal.markrecap.ui.lookup.SealLookupViewModel
import weddellseal.markrecap.ui.utils.getCurrentYear
import weddellseal.markrecap.viewmodelfactories.HomeViewModelFactory
import weddellseal.markrecap.viewmodelfactories.SealLookupViewModelFactory

/**
 * On-device regression for Parker 2025 season recap (lookup): last-seen population must always
 * show, including when GPS is at White Island. Highlight only when the GPS colony's population
 * differs (Erebus Bay vs White Island).
 *
 * JVM [LookupCardWhiteIslandPopulationTest] mounts LookupCard with a fake seal. This test seeds
 * WedCheck, searches on Seal Lookup, and pins GPS colony on the activity [HomeViewModel].
 */
@RunWith(AndroidJUnit4::class)
class SealLookupWhiteIslandPopulationInstrumentedTest {

    companion object {
        private const val TEST_TAG_ID = "9881C"
        private const val TEST_SPENO = 39999
        private const val WHITE_ISLAND_SEAL_POPULATION = "White Island"
        private const val EREBUS_BAY_COLONY = "Turtle Rock"
        private const val PHOTO_PROMPT = "Please take a photo of the tags and seal!"
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
    fun lookupWhiteIslandSeal_showsPopulation_andHighlightsOnlyWhenGpsPopulationDiffers() {
        seedWhiteIslandWedCheckSeal()
        navigateToSealLookup()
        searchForTag(TEST_TAG_ID)
        waitUntilLookupShowsSpeno()

        pinGpsColony(ColonyPopulation.NOT_DETECTED)
        waitUntilDisplayed("Population")
        waitUntilDisplayed(WHITE_ISLAND_SEAL_POPULATION)
        waitUntilDoesNotExist(mismatchBanner(ColonyPopulation.NOT_DETECTED))
        waitUntilDoesNotExist(PHOTO_PROMPT)

        pinGpsColony(ColonyPopulation.WHITE_ISLAND)
        waitUntilDisplayedWithPinnedColony(ColonyPopulation.WHITE_ISLAND, "Population")
        waitUntilDisplayedWithPinnedColony(
            ColonyPopulation.WHITE_ISLAND,
            WHITE_ISLAND_SEAL_POPULATION,
        )
        waitUntilDoesNotExistWithPinnedColony(
            ColonyPopulation.WHITE_ISLAND,
            mismatchBanner(ColonyPopulation.WHITE_ISLAND),
        )
        waitUntilDoesNotExistWithPinnedColony(ColonyPopulation.WHITE_ISLAND, PHOTO_PROMPT)

        pinGpsColony(EREBUS_BAY_COLONY)
        waitUntilDisplayedWithPinnedColony(EREBUS_BAY_COLONY, "Population")
        waitUntilDisplayedWithPinnedColony(
            EREBUS_BAY_COLONY,
            mismatchBanner(EREBUS_BAY_COLONY),
            substring = true,
        )
        waitUntilDisplayedWithPinnedColony(EREBUS_BAY_COLONY, PHOTO_PROMPT)
    }

    private fun mismatchBanner(currentColonyLocation: String): String =
        "This seal was last seen at $WHITE_ISLAND_SEAL_POPULATION. " +
            "Current colony detected by device is $currentColonyLocation."

    private fun seedWhiteIslandWedCheckSeal() {
        runBlocking {
            val fileUploadId = app.getFileUploadDao().insertFileUpload(
                FileUploadEntity(
                    fileType = FileType.WEDCHECK,
                    fileAction = FileAction.UPLOAD.name,
                    filename = "instrumented-white-island-wedcheck.csv",
                    status = FileStatus.IDLE,
                    statusMessage = null,
                    recordCount = 0,
                ),
            )
            app.getWedCheckDao().insertWedCheckRecord(
                WedCheckRecord(
                    speno = TEST_SPENO,
                    season = getCurrentYear(),
                    ageClass = "A",
                    sex = "M",
                    tagIdOne = TEST_TAG_ID,
                    tagIdTwo = TEST_TAG_ID,
                    comments = "",
                    ageYears = 9,
                    tissueSampled = "NA",
                    pupinMassStudy = "NA",
                    numPreviousPups = "NA",
                    pupinTTStudy = "NA",
                    momMassMeasurements = "NA",
                    condition = "3",
                    lastPhysio = "NA",
                    population = WHITE_ISLAND_SEAL_POPULATION,
                    fileUploadId = fileUploadId,
                    latitude = -78.079,
                    longitude = 167.288,
                ),
            )
            val seeded = app.getWedCheckDao().lookupSealByTagID(TEST_TAG_ID)
            assertEquals(TEST_SPENO, seeded.speno)
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
        // Do not wait for "Seal Lookup": that string is both the drawer label and the app bar
        // title, and the closed drawer stays composed, so onNodeWithText never matches one node.
        waitUntilNodeExists(SEARCH_FIELD_LABEL, substring = true)
    }

    private fun searchForTag(tagId: String) {
        // Label/placeholder live on child Text nodes in the unmerged tree, not on the
        // SetText node. Lookup has a single text field, so match that action alone.
        // Do not performClick first: that opens the IME and can cover the Search icon
        // on the default 320x640 CI AVD before onClick hides the keyboard.
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

    /**
     * Search icon onClick can miss under the IME. If the card never appears, run the same
     * lookup the button would have called so the population assertions still execute.
     */
    private fun waitUntilLookupShowsSpeno() {
        val speno = TEST_SPENO.toString()
        val foundInTree = try {
            composeRule.waitUntil(timeoutMillis = 8_000) { nodeExists(speno) }
            true
        } catch (_: ComposeTimeoutException) {
            false
        }
        if (!foundInTree) {
            composeRule.runOnUiThread {
                getSealLookupViewModel().findSealbyTagID(TEST_TAG_ID)
            }
            composeRule.waitForIdle()
        }
        waitUntilDisplayed(speno)
    }

    private fun nodeExists(text: String, substring: Boolean = false): Boolean =
        composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun getHomeViewModel(): HomeViewModel {
        val activity = composeRule.activity
        val factory = HomeViewModelFactory(
            FusedLocationSource(activity),
            SealColonyRepository(app.getSealColoniesDao()),
            ObserversRepository(app.getObserversDao()),
        )
        return ViewModelProvider(activity, factory)[HomeViewModel::class.java]
    }

    private fun sampleColony(location: String) = SealColony(
        inOut = "in",
        location = location,
        nLimit = 45.0,
        sLimit = 40.0,
        wLimit = 30.0,
        eLimit = 35.0,
        adjLat = -77.5,
        adjLong = 166.5,
        fileUploadId = 1L,
    )

    private fun pinGpsColony(location: String) {
        val colony = sampleColony(location)
        composeRule.runOnUiThread {
            getHomeViewModel().setAutoDetectedColony(colony)
        }
        composeRule.waitForIdle()
    }

    private fun waitUntilNodeExists(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitUntilDisplayed(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            nodeIsOnScreenOrInTree(text, substring)
        }
    }

    /**
     * Scrolls matching nodes into view. [assertIsDisplayed] fails when LookupCard is clipped
     * on a tiny CI AVD (default 320x640); composed-and-scrolled is enough for this regression.
     */
    private fun nodeIsOnScreenOrInTree(text: String, substring: Boolean): Boolean {
        val nodes = composeRule.onAllNodes(
            hasText(text, substring = substring),
            useUnmergedTree = true,
        )
        val count = nodes.fetchSemanticsNodes().size
        if (count == 0) return false
        return (0 until count).any { index ->
            val node = nodes[index]
            try {
                node.performScrollTo()
            } catch (_: AssertionError) {
                // Not in a scrollable parent.
            }
            try {
                node.assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                true
            }
        }
    }

    private fun waitUntilDoesNotExist(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    /**
     * Re-pins GPS colony while waiting. Location updates can overwrite [HomeViewModel]'s
     * auto-detected colony after the test sets it.
     */
    private fun waitUntilDisplayedWithPinnedColony(
        location: String,
        text: String,
        substring: Boolean = false,
    ) {
        val colony = sampleColony(location)
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.runOnUiThread {
                getHomeViewModel().setAutoDetectedColony(colony)
            }
            nodeIsOnScreenOrInTree(text, substring)
        }
    }

    private fun waitUntilDoesNotExistWithPinnedColony(
        location: String,
        text: String,
        substring: Boolean = false,
    ) {
        val colony = sampleColony(location)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.runOnUiThread {
                getHomeViewModel().setAutoDetectedColony(colony)
            }
            composeRule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }
}
