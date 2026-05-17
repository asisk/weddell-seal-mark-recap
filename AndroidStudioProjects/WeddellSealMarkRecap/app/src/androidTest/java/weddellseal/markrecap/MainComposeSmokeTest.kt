package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainComposeSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private fun openDrawer() {
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    private fun clickDrawerItem(text: String) {
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performScrollTo()
            .performClick()
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
     * Waits until [text] is scrolled into view and passes [assertIsDisplayed].
     * Needed because [waitForText] only checks semantics presence, not visibility.
     */
    private fun waitUntilDisplayed(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            try {
                composeRule.onNodeWithText(text, substring = substring)
                    .performScrollTo()
                    .assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun openDrawer_navigateToTagRetag_showsHeader() {
        openDrawer()
        clickDrawerItem("Tag/Retag")
        composeRule.onNodeWithText("Tag / Retag", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun openDrawer_navigateToAdminImport_showsManageImports() {
        openDrawer()
        clickDrawerItem("Admin Actions")
        waitForText("Data Management")
        clickDrawerItem("Data Management")
        waitUntilDisplayed("Administration")
        // Rail icon content descriptions are stable; label text "Import" also appears on card buttons.
        composeRule.onNode(hasContentDescription("Import") and hasClickAction())
            .performClick()
        composeRule.waitForIdle()
        waitUntilDisplayed("Manage Imports")
        waitUntilDisplayed("WedCheck File", substring = true)
    }

    @Test
    fun navigateToRecentEntries_showsEmptyState() {
        openDrawer()
        clickDrawerItem("Recent Entries")
        waitForText("Recent Observations")
        composeRule.onNodeWithText("No records to display.", substring = true)
            .assertIsDisplayed()
    }
}
