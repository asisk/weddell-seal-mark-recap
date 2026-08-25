package weddellseal.markrecap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation

/**
 * Lookup highlight: last-seen population vs GPS colony.
 */
@Composable
fun PopulationMismatchBanner(
    sealPopulation: String,
    currentColonyLocation: String?,
    showPopulationRow: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE0B2))
            .padding(14.dp),
    ) {
        Text(
            "This seal was last seen at $sealPopulation. " +
                "Current colony detected by device is $currentColonyLocation."
        )
        if (sealPopulation.equals(ColonyPopulation.WHITE_ISLAND, ignoreCase = true)) {
            Text(
                "Please take a photo of the tags and seal!",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (showPopulationRow) {
            DataDisplayRow("Population", sealPopulation)
        }
    }
}
