package weddellseal.markrecap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueObservation

@Composable
fun ObservationItem(
    onEditDo: (ObservationRecord) -> Unit,
    onViewDo: (ObservationRecord) -> Unit,
    observation: ObservationRecord,
    pupOne: ObservationRecord? = null,
    pupTwo: ObservationRecord? = null
) {
    // State to control the visibility of the dropdown menu
    var expanded by remember { mutableStateOf(false) }

    // Row to display the observation notebook string, date entered, & the three-dot menu
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(start = 40.dp),
        ) {
            Text(
                text =
                    notebookEntryValueObservation(observation) +
                            "    " + observation.date + " " + observation.time + "    ",
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            pupOne?.let { record ->
                Text(
                    text =
                        notebookEntryValueObservation(record),
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            pupTwo?.let { record ->
                Text(
                    text =
                        notebookEntryValueObservation(record),
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        if (observation.ageClass == SealAgeClass.PUP.description) {
            Icon(
                painter = painterResource(R.drawable.ic_pup_foreground),
                contentDescription = "Pup",
            )
        } else if (observation.numRelatives > "0") {
            Icon(
                painter = painterResource(R.mipmap.ic_mom_pup_foreground),
                contentDescription = "Mom and pup",
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_adult_foreground),
                contentDescription = "Adult or Yearling",
            )
        }

        // Three-dot menu
        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert, // Three-dot icon
                    contentDescription = "More options",
                    Modifier.size(36.dp)
                )
            }

            // Dropdown menu with options
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { onEditDo(observation) })

                DropdownMenuItem(
                    text = { Text("View") },
                    onClick = { onViewDo(observation) })
            }
        }
    }
}