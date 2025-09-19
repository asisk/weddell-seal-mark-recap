package weddellseal.markrecap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.navigation.NavHostController
import weddellseal.markrecap.Screens
import weddellseal.markrecap.ui.recentobservations.DisplayObservation
import weddellseal.markrecap.ui.recentobservations.RecentObservationsViewModel
import weddellseal.markrecap.ui.tagretag.TagRetagViewModel

@Composable
fun RecentObservations(
    viewModel: TagRetagViewModel,
    recentObsViewModel: RecentObservationsViewModel,
    navController: NavHostController,
) {
    val displayObservations by recentObsViewModel.displayObservations.collectAsState()

    if (displayObservations.isEmpty()) {

        Box {
            Text(
                text = "No records to display.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color.Gray
            )
        }

    } else {

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // display individual records for observations
            // those with pups will be displayed in one row
            // if they have no relatives they will have their own row
            items(displayObservations) { displayObs ->
                when (displayObs) {
                    is DisplayObservation.WithPups -> {
                        ObservationItem(
                            onEditDo = {
                                // If the technician has a partially complete observation,
                                // prevent them from editing
                                viewModel.onEditAttempt(displayObs)
                            },
                            onViewDo = {
                                viewModel.onViewAttempt(displayObs)
                                navController.navigate(Screens.ObservationViewer.route)
                            },
                            observation = displayObs.primarySeal,
                            pupOne = displayObs.pupOne,
                            pupTwo = displayObs.pupTwo
                        )

                        HorizontalDivider()
                    }

                    is DisplayObservation.Standalone -> {
                        ObservationItem(
                            onEditDo = {
                                // If the technician has a partially complete observation,
                                // prevent them from editing
                                viewModel.onEditAttempt(displayObs)
                            },
                            onViewDo = {
                                viewModel.onViewAttempt(displayObs)
                                navController.navigate(Screens.ObservationViewer.route)
                            },
                            observation = displayObs.primarySeal,
                            pupOne = null,
                            pupTwo = null
                        )

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}