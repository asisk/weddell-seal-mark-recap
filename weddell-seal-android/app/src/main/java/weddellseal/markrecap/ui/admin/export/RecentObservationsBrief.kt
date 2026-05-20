package weddellseal.markrecap.ui.admin.export

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel

@Composable
fun RecentObservationsBrief(
    recentObsViewModel: RecentObservationsViewModel,
) {
    val exportObservations by recentObsViewModel.exportCurrentObservations.collectAsState()
    Box(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .animateContentSize()
            .border(4.dp, Color.LightGray)
    ) {

        if (exportObservations.isEmpty()) {
            Text(
                text = "No records to display.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color.Gray
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // display individual records for observations
            // those with pups will be displayed in one row
            // if they have no relatives they will have their own row
            items(exportObservations) { displayObs ->
                ObservationItemBrief(observation = displayObs)

                HorizontalDivider()
            }
        }
    }
}