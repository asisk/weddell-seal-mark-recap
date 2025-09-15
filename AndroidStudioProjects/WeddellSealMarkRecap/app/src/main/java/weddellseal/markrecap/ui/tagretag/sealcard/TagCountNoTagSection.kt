package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.tagretag.SegmentedButtonGroup

// ---- Reused constants (don’t rebuild every recomposition) ----
private val NUM_TAGS_OPTIONS = listOf("1", "2")

@Composable
fun TagCountNoTagSection(
    isNoTag: Boolean,
    numTags: String,
    onNumTagsSelected: (String) -> Unit,
    onToggleNoTag: (Boolean) -> Unit,
    showOldTagMarks: Boolean,
    oldTagMarks: Boolean,
    onToggleOldTagMarks: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // NUMBER OF TAGS
        if (!isNoTag) {
            Text(
                "# of Tags",
                style = MaterialTheme.typography.titleLarge
            )

            SegmentedButtonGroup(
                options = NUM_TAGS_OPTIONS,
                selectedOption = numTags,
                onOptionSelected = { newVal ->
                    onNumTagsSelected(newVal)
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // NO TAG
            Text(
                text = "No Tag",
                style = MaterialTheme.typography.titleLarge,
            )

            Checkbox(
                checked = isNoTag,
                onCheckedChange = {
                    onToggleNoTag(it)
                },
            )
        }

        // OLD TAG MARKS, FOR NEW TAG EVENT ONLY
        if (showOldTagMarks) {
            Box(
                modifier = Modifier.weight(1f), // take the remaining space
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Old Tag Marks",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Checkbox(
                        checked = oldTagMarks,
                        onCheckedChange = {
                            onToggleOldTagMarks(it)
                        },
                    )
                }
            }
        }
    }
}