package weddellseal.markrecap

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Test
    fun openDrawer_navigateToTagRetag_showsHeader() {
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithText("Tag/Retag", substring = true, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tag / Retag", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun openDrawer_navigateToAdminImport_showsManageImports() {
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithText("Admin Actions").performClick()
        composeRule.onNodeWithText("Data Management").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Import").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Manage Imports", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("WedCheck File", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun navigateToRecentEntries_showsEmptyState() {
        composeRule.onNode(hasContentDescription("Toggle drawer"), useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithText("Recent Entries").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No records to display.", substring = true)
            .assertIsDisplayed()
    }
}
