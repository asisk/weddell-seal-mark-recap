package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.ui.utils.notebookEntryValueObservation

@Composable
fun ObservationItem(
    onEditDo: (ObservationLogEntry) -> Unit,
    onViewDo: (ObservationLogEntry) -> Unit,
    observation: ObservationLogEntry
) {
    // State to control the visibility of the dropdown menu
    var expanded by remember { mutableStateOf(false) }

    // Row to display the observation and the three-dot menu
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display the observation details
        Column {
            Text(
                text =
                notebookEntryValueObservation(observation) +
                        "    " + observation.date + " " + observation.time + "    ",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // Three-dot menu
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert, // Three-dot icon
                    contentDescription = "More options"
                )
            }

            // Dropdown menu with options
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit")},
                    onClick = { onEditDo(observation) })

                DropdownMenuItem(
                    text = { Text("View") },
                    onClick = { onViewDo(observation) })
            }
        }
    }
}