package weddellseal.markrecap.ui.admin.export

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord
import weddellseal.markrecap.ui.tagretag.utils.notebookEntryValueObservation

@Composable
fun ObservationItemBrief(
    observation: ObservationRecord,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = observation.id.toString(),
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.width(24.dp))

        Text(
            text = observation.date + " " + observation.time + "    ",
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.width(24.dp))

        Text(
            text = notebookEntryValueObservation(observation),
            modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}