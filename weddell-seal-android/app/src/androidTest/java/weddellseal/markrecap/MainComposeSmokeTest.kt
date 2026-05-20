package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import weddellseal.markrecap.InstrumentedComposeTestSupport.waitForComposeReady
import weddellseal.markrecap.ui.admin.AdminViewModel
import weddellseal.markrecap.viewmodelfactories.AdminViewModelFactory

@RunWith(AndroidJUnit4::class)
class MainComposeSmokeTest {

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

    private fun openDrawer() {
        composeRule.waitForComposeReady()
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    private fun clickDrawerItem(text: String) {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true)
        scrollIntoViewIfPossible(node)
        node.performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Polls until [text] is displayed. Uses the merged semantics tree so visibility matches what
     * users see. Optionally scrolls when [text] is inside a verticalScroll container.
     */
    private fun waitUntilDisplayed(
        text: String,
        substring: Boolean = false,
        scrollIntoView: Boolean = false,
    ) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            try {
                val node = composeRule.onNodeWithText(text, substring = substring)
                if (scrollIntoView) {
                    try {
                        node.performScrollTo()
                    } catch (_: AssertionError) {
                        // Not in a scrollable parent; still check display below.
                    }
                }
                node.assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    private fun scrollIntoViewIfPossible(node: SemanticsNodeInteraction) {
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {
            // Drawer items are not always under a scroll semantics parent in the unmerged tree.
        }
    }

    /** Clears observation rows so Recent Observations shows a deterministic empty state. */
    private fun clearAllObservations() {
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ObservationLogApplication
        runBlocking {
            app.observationRepo.deleteAll()
        }
    }

    private fun navigateToAdminViaDrawer() {
        openDrawer()
        clickDrawerItem("Admin Actions")
        waitForText("Data Management")
        clickDrawerItem("Data Management")
    }

    /**
     * Selects an admin navigation-rail tab by index (Home=0, Dashboard=1, Import=2, …).
     *
     * Rail [performClick] is unreliable in Compose UI tests across API levels and form factors,
     * so tests that need a specific admin tab should drive [AdminViewModel] directly after
     * navigating to the admin screen through the drawer.
     */
    private fun selectAdminNavRailTab(selection: Int) {
        val activity = composeRule.activity
        val app = activity.application as ObservationLogApplication
        val factory = AdminViewModelFactory(app, app.supportingDataRepo)
        val adminViewModel = ViewModelProvider(activity, factory)[AdminViewModel::class.java]
        composeRule.runOnUiThread {
            adminViewModel.setNavRailSelection(selection)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun openDrawer_navigateToTagRetag_showsHeader() {
        openDrawer()
        clickDrawerItem("Tag/Retag")
        composeRule.onNodeWithText("Tag / Retag", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun openDrawer_navigateToAdmin_showsDashboard() {
        navigateToAdminViaDrawer()
        waitUntilDisplayed("Administration")
    }

    @Test
    fun adminImportTab_showsContent() {
        navigateToAdminViaDrawer()
        // Wait for the admin route to settle before changing tabs (split from dashboard test).
        waitUntilDisplayed("Administration")
        selectAdminNavRailTab(selection = 2)
        waitUntilDisplayed("Manage Imports")
        // Card labels can be composed but off-screen on narrow phones; presence is sufficient.
        waitForText("WedCheck File")
    }

    @Test
    fun navigateToRecentEntries_showsEmptyState() {
        clearAllObservations()
        openDrawer()
        clickDrawerItem("Recent Entries")
        waitUntilDisplayed("Recent Observations")
        waitUntilDisplayed("No records to display.", substring = true)
    }
}
