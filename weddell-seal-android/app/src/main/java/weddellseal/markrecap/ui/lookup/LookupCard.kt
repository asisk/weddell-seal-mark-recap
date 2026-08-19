package weddellseal.markrecap.ui.lookup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.ColonyPopulation
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.ui.DataDisplayRow
import weddellseal.markrecap.ui.PopulationMismatchBanner
import weddellseal.markrecap.ui.home.HomeViewModel

@Composable
fun LookupCard(
    seal: WedCheckSeal,
    homeViewModel: HomeViewModel
) {
    val scrollState = rememberScrollState()
    val autoDetectedColony by homeViewModel.autoDetectedColony.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 80.dp,
                end = 90.dp,
                top = 10.dp,
                bottom = 40.dp
            )
            .verticalScroll(state = scrollState, enabled = true)
    ) {// Apply padding to left and right

        DataDisplayRow("SPENO", if (seal.speNo == 0) "" else seal.speNo.toString())

        DataDisplayRow("Tag 1", seal.tagIdOne)
        DataDisplayRow("Tag 2", seal.tagIdTwo)

        DataDisplayRow("Age Class", seal.ageClass.alpha)

        DataDisplayRow("Age Years", seal.ageYears)

        DataDisplayRow("Sex", seal.sex.alpha)

        if (seal.tissueSampled == "Need") {
            // BANNER For White Island Seals that are observed outside of White Island colony
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE0B2))
                    .padding(14.dp),
            ) {
                DataDisplayRow("Tissue Taken", seal.tissueSampled)
            }
        } else {
            DataDisplayRow("Tissue Taken", seal.tissueSampled)
        }

        if (seal.condition.description == "Dead") {
            // BANNER For White Island Seals that are observed outside of White Island colony
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE0B2))
                    .padding(14.dp),
            ) {
                DataDisplayRow("Condition", seal.condition.code)
            }
        } else {
            DataDisplayRow("Condition", seal.condition.code)
        }

        DataDisplayRow("Last Physio", seal.lastPhysio)

        DataDisplayRow(
            "Last Seen",
            if (seal.lastSeenSeason == 0) "" else seal.lastSeenSeason.toString()
        )

        // Parker 2025 season recap (lookup): always show last-seen population. Highlight only
        // when GPS has a colony and that colony's population differs. Ignore colony override.
        val highlightPopulation = ColonyPopulation.shouldHighlightMismatch(
            seal.population,
            autoDetectedColony?.location,
        )
        if (highlightPopulation) {
            PopulationMismatchBanner(
                sealPopulation = seal.population,
                currentColonyLocation = autoDetectedColony?.location,
                showPopulationRow = true,
            )
        } else {
            DataDisplayRow("Population", seal.population)
        }

        DataDisplayRow(
            "Lat",
            if (seal.latitude == 0.0) "" else seal.latitude.toString()
        )

        DataDisplayRow(
            "Long",
            if (seal.longitude == 0.0) "" else seal.longitude.toString()
        )

        DataDisplayRow("Previous Pups", seal.numPreviousPups)

        DataDisplayRow("Mass Pups", seal.massPups)

        DataDisplayRow("Swim Pups", seal.pupinTTStudy)

        DataDisplayRow("Photo Years", seal.momMassMeasurements)

        DataDisplayRow("Comments", seal.comment)
    }
}
