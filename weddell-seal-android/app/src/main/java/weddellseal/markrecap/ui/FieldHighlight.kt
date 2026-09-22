package weddellseal.markrecap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * High-contrast field highlight for lookup notes, tissue Need, Dead, and WedCheck comments.
 *
 * Matches selected tag/retag buttons (black background, white text) so the cue is readable
 * in bright field conditions. Pale yellow failed that test.
 */
object FieldHighlight {
    const val TEST_TAG = "fieldHighlight"
    val Background = Color.Black
    val Content = Color.White
}

@Composable
fun FieldHighlightBanner(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(FieldHighlight.TEST_TAG),
        color = FieldHighlight.Background,
        contentColor = FieldHighlight.Content,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content,
        )
    }
}
