package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for tag/speno not updating when the user edits Tag ID and taps Save
 * without leaving the field (blur). [TagIdSection] only calls [onNumberCommitted] when
 * the number field loses focus.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TagIdSectionCommitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tagNumber_isNotCommittedWhileFieldRemainsFocused() {
        var committedNumber: String? = null

        composeRule.setContent {
            MaterialTheme {
                TagIdSection(
                    label = "Tag ID",
                    number = "456",
                    alpha = "B",
                    onClear = {},
                    onNumberCommitted = { committedNumber = it },
                    onAlphaSelected = {},
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithText("456").performClick()
        composeRule.onNodeWithText("456").assertIsFocused()
        composeRule.onNodeWithText("456").performTextReplacement("789")

        composeRule.waitForIdle()
        assertNotEquals("789", committedNumber)
    }

    @Test
    fun tagNumber_isCommittedWhenFieldLosesFocus() {
        var committedNumber: String? = null

        composeRule.setContent {
            MaterialTheme {
                TagIdSection(
                    label = "Tag ID",
                    number = "456",
                    alpha = "B",
                    onClear = {},
                    onNumberCommitted = { committedNumber = it },
                    onAlphaSelected = {},
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithText("456").performClick()
        composeRule.onNodeWithText("456").performTextReplacement("789")
        // Tapping an alpha button clears focus from the number field (see SingleSelectTagAlphaButtonGroup).
        composeRule.onNodeWithText("A").performClick()

        composeRule.waitForIdle()
        assertEquals("789", committedNumber)
    }
}
