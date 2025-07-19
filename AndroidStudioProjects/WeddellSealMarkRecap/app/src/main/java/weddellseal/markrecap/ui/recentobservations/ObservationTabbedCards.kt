package weddellseal.markrecap.ui.recentobservations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.ui.tagretag.ObservationTabItem
import weddellseal.markrecap.ui.tagretag.TagRetagModel

/**
 * TabbedCards responds to changes in the model for each seal.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationTabbedCards(
    tagRetagModel: TagRetagModel,
) {
    val selectedObservation by tagRetagModel.selectedRecentObservation.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tabItems by remember {
        mutableStateOf(createTabItems(selectedObservation))
    }

    // Update the tab items when the seals change
    LaunchedEffect(selectedObservation) {
        tabItems = createTabItems(selectedObservation)

        // Ensure selectedTabIndex is within bounds after updating the list
        if (selectedTabIndex >= tabItems.size) {
            selectedTabIndex = tabItems.lastIndex.coerceAtLeast(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tabItem.title,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(0f)
        ) {

            // CONTENT
            if (tabItems.isNotEmpty()) {
                tabItems[selectedTabIndex].content()
            }
        }

    }
}

fun createTabItems(
    observation: DisplayObservation?
): List<ObservationTabItem> {
    val items = mutableListOf<ObservationTabItem>()


    when (observation) {
        is DisplayObservation.WithPups -> {
            items.add(ObservationTabItem(SealType.PRIMARY.label, observation.primarySeal) {
                ObservationEntryCard(observation.primarySeal)
            })
            observation.pupOne?.let {
                items.add(ObservationTabItem(SealType.PUPONE.label, it) {
                    ObservationEntryCard(it)
                })
            }
            observation.pupTwo?.let {
                items.add(ObservationTabItem(SealType.PUPTWO.label, it) {
                    ObservationEntryCard(it)
                })
            }
        }

        is DisplayObservation.Standalone -> {
            items.add(ObservationTabItem(SealType.PRIMARY.label, observation.primarySeal) {
                ObservationEntryCard(observation.primarySeal)
            })
        }

        null -> {
            // No observation selected, do nothing
        }
    }

    return items
}
