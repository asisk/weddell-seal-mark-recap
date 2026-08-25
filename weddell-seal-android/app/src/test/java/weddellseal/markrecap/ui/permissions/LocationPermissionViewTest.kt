package weddellseal.markrecap.ui.permissions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationPermissionViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclosure_showsWhoWhatWhyAndOnDeviceRetention() {
        composeRule.setContent {
            MaterialTheme {
                LocationPermissionView(onLocationGranted = {}, onSkip = {})
            }
        }

        composeRule.onNodeWithText(LOCATION_DISCLOSURE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(LOCATION_DISCLOSURE_INTRO, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(LOCATION_DISCLOSURE_STAMP, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(LOCATION_DISCLOSURE_COLONY, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(LOCATION_DISCLOSURE_RETENTION, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
        composeRule.onNodeWithText("Open Settings").assertDoesNotExist()
    }

    @Test
    fun notNow_skipsWithoutGranting() {
        var skipped = false

        composeRule.setContent {
            MaterialTheme {
                LocationPermissionView(
                    onLocationGranted = {},
                    onSkip = { skipped = true },
                )
            }
        }

        composeRule.onNodeWithText("Not now").performClick()
        composeRule.waitForIdle()
        assertTrue(skipped)
    }
}
