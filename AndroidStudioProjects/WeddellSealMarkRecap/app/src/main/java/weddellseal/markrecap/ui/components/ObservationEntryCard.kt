package weddellseal.markrecap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.ObservationLogEntry
import weddellseal.markrecap.ui.utils.notebookEntryValueObservation

@Composable
fun ObservationEntryCard(
    observation: ObservationLogEntry
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text =
            notebookEntryValueObservation(observation) +
                    "    " + observation.date + " " + observation.time + "    ",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.headlineMedium,
        )

        DataDisplayRow("Season", observation.season)

        DataDisplayRow("SpeNo", observation.speno)

        DataDisplayRow("Date", observation.time)

        DataDisplayRow("Time", observation.date)

        DataDisplayRow("Census", observation.censusID)

        DataDisplayRow("Latitude", observation.latitude)

        DataDisplayRow("Longitude", observation.longitude)

        DataDisplayRow("Age Class", observation.ageClass)

        DataDisplayRow("Sex", observation.sex)

        DataDisplayRow("Number of Relatives", observation.numRelatives)

        DataDisplayRow("Old Tag 1", observation.oldTagIDOne)

        DataDisplayRow("Old Tag 2", observation.oldTagIDTwo)

        DataDisplayRow("Tag 1", observation.tagIDOne)

        DataDisplayRow("Tag 2", observation.tagIDTwo)

        DataDisplayRow("Relative 1 Tag", observation.relativeTagIDOne)

        DataDisplayRow("Relative 2 Tag", observation.relativeTagIDTwo)

        DataDisplayRow("Condition", observation.sealCondition)

        DataDisplayRow("Observers", observation.observerInitials)

        DataDisplayRow("Tag Event", observation.tagEvent)

        DataDisplayRow("Flagged Entry", observation.flaggedEntry)

        DataDisplayRow("Weight", observation.weight)

        DataDisplayRow("Tissue", observation.tissueSampled)

        DataDisplayRow("Comments", observation.comments)

        DataDisplayRow("Colony", observation.colony)
    }
}
