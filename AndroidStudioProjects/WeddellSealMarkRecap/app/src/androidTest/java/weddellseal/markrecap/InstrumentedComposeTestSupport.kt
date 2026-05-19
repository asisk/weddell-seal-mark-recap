package weddellseal.markrecap

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/**
 * Helpers for instrumented Compose tests.
 *
 * [ruleChain] grants runtime permissions before [AndroidComposeTestRule] launches [MainActivity],
 * avoiding races where the UI requests permissions before the Compose hierarchy is ready.
 */
object InstrumentedComposeTestSupport {
    private const val DRAWER_TOGGLE_DESCRIPTION = "Toggle drawer"

    fun ruleChain(
        grantPermissionRule: GrantPermissionRule,
        composeRule: AndroidComposeTestRule<*, *>,
    ): TestRule = RuleChain
        .outerRule(grantPermissionRule)
        .around(composeRule)

    /**
     * Blocks until [MainActivity] has called setContent and the home drawer control is in the tree.
     * Polls safely while the hierarchy is not yet attached (common on slower devices).
     */
    fun AndroidComposeTestRule<*, *>.waitForComposeReady(timeoutMillis: Long = 20_000) {
        waitUntil(timeoutMillis) {
            try {
                onAllNodes(
                    hasContentDescription(DRAWER_TOGGLE_DESCRIPTION),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().isNotEmpty()
            } catch (_: IllegalStateException) {
                false
            }
        }
        waitForIdle()
    }
}
