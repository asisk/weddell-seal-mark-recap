package weddellseal.markrecap.ui.lookup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.WedCheckSeal
import weddellseal.markrecap.ui.DataDisplayRow

@Composable
fun LookupCard(
    seal: WedCheckSeal
) {
    val scrollState = rememberScrollState()

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

        // SPENO
        DataDisplayRow("SPENO", if (seal.speNo == 0) "" else seal.speNo.toString())

        // TAGS
        DataDisplayRow("Tag 1", seal.tagIdOne)
        DataDisplayRow("Tag 2", seal.tagIdTwo)

        // AGE
        DataDisplayRow("Age Class", seal.age)

        // AGE YEARS
        DataDisplayRow("Age Years", seal.ageYears)

        // SEX
        DataDisplayRow("Sex", seal.sex)

        // TISSUE SAMPLED
        DataDisplayRow("Tissue Taken", seal.tissueSampled)

        // CONDITION
        DataDisplayRow("Condition", seal.condition)

        // PREVIOUS PUPS
        DataDisplayRow("Last Physio", seal.lastPhysio)

        // LAST SEEN
        DataDisplayRow(
            "Last Seen",
            if (seal.lastSeenSeason == 0) "" else seal.lastSeenSeason.toString()
        )

        // PREVIOUS PUPS
        DataDisplayRow("Colony", seal.colony)

        // PREVIOUS PUPS
        DataDisplayRow("Previous Pups", seal.numPreviousPups)

        // MASS PUPS
        DataDisplayRow("Mass Pups", seal.massPups)

        // SWIM PUPS
        DataDisplayRow("Swim Pups", seal.pupinTTStudy)

        // PHOTO YEARS
        DataDisplayRow("Photo Years", seal.momMassMeasurements)

        // COMMENTS
        DataDisplayRow("Comments", seal.comment)
    }
}
