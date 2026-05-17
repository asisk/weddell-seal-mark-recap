package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
 * Regression tests for tag ID commit and [TagIDOutlinedTextField] model sync:
 * - [TagIdSection] only calls [onNumberCommitted] on blur.
 * - While focused, in-progress text must survive parent recomposition when the model
 *   still holds the last committed number ([TagIDOutlinedTextField] LaunchedEffect guard).
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

    /**
     * Parent still passes number = "456" while the user has typed 789 with focus. An unrelated
     * state change forces recomposition; the field must not snap back to 456.
     */
    @Test
    fun tagNumber_retainsInProgressEditWhenParentRecomposesWhileFocused() {
        val number = mutableStateOf("456")
        val recomposeKey = mutableIntStateOf(0)

        composeRule.setContent {
            TagIdSectionHost(
                number = number.value,
                recomposeKey = recomposeKey.intValue,
                onNumberCommitted = {},
            )
        }

        composeRule.onNodeWithText("456").performClick()
        composeRule.onNodeWithText("456").performTextReplacement("789")
        composeRule.onNodeWithText("789").assertIsFocused()

        recomposeKey.intValue = 1
        composeRule.waitForIdle()

        composeRule.onNodeWithText("789").assertIsDisplayed()
        composeRule.onNodeWithText("456").assertDoesNotExist()
    }

    /** When the field is not focused, a model update should replace what the user sees. */
    @Test
    fun tagNumber_syncsFromModelWhenNotFocused() {
        val number = mutableStateOf("456")

        composeRule.setContent {
            TagIdSectionHost(
                number = number.value,
                recomposeKey = 0,
                onNumberCommitted = {},
            )
        }

        composeRule.onNodeWithText("456").assertIsDisplayed()
        number.value = "123"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("123").assertIsDisplayed()
    }
}

@Composable
private fun TagIdSectionHost(
    number: String,
    recomposeKey: Int,
    onNumberCommitted: (String) -> Unit,
) {
    MaterialTheme {
        Column {
            Text("key=$recomposeKey")
            TagIdSection(
                label = "Tag ID",
                number = number,
                alpha = "B",
                onClear = {},
                onNumberCommitted = onNumberCommitted,
                onAlphaSelected = {},
                modifier = Modifier,
            )
        }
    }
}
