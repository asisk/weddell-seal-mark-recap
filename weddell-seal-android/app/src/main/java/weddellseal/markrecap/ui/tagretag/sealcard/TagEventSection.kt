package weddellseal.markrecap.ui.tagretag.sealcard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.TagEventType
import weddellseal.markrecap.ui.tagretag.SegmentedButtonGroup

// ---- Reused constants (don’t rebuild every recomposition) ----
private val TAG_EVENT_OPTIONS =
    TagEventType.values().filter { it != TagEventType.UNKNOWN }.map { it.description }

@Composable
fun TagEventSection(
    isNoTag: Boolean,
    seal: Seal,
    onSelectTagEvent: (TagEventType) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tag Event", style = MaterialTheme.typography.titleLarge)

        if (isNoTag) {

            // the database record needs to have an event type of marked for Retag
            Text(
                TagEventType.MARKED.description,
                style = MaterialTheme.typography.titleLarge
            )

        } else {

            SegmentedButtonGroup(
                options = TAG_EVENT_OPTIONS,
                selectedOption = seal.tagEventType.description,
                onOptionSelected = { onSelectTagEvent(TagEventType.fromSelection(it)) }
            )
        }
    }
}