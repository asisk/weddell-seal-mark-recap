package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun waitForNodeWithText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun openDrawer_navigateToTagRetag_showsHeader() {
        openDrawer()
        composeRule.onNodeWithText("Tag/Retag", substring = true, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tag / Retag", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun openDrawer_navigateToAdminImport_showsManageImports() {
        openDrawer()
        composeRule.onNodeWithText("Admin Actions", useUnmergedTree = true).performClick()
        waitForNodeWithText("Data Management")
        composeRule.onNodeWithText("Data Management", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        waitForNodeWithText("Dashboard")
        composeRule.onNodeWithContentDescription("Import").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manage Imports", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("WedCheck File", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun navigateToRecentEntries_showsEmptyState() {
        openDrawer()
        composeRule.onNodeWithText("Recent Entries", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        waitForNodeWithText("Recent Observations")
        waitForNodeWithText("No records to display.", substring = true)
        composeRule.onNodeWithText("No records to display.", substring = true)
            .assertIsDisplayed()
    }
}
