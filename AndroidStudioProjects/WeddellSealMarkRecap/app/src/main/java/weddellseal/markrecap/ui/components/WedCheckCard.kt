package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.WedCheckSeal

@Composable
fun WedCheckCard(
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
        WedCheckDataDisplayRow("SPENO", if (seal.speNo == 0) "" else seal.speNo.toString())

        // TAG ID
        WedCheckDataDisplayRow("Tag", seal.tagId)

        // AGE
        WedCheckDataDisplayRow("Age Class", seal.age)

        // AGE YEARS
        WedCheckDataDisplayRow("Age Years", seal.ageYears)

        // SEX
        WedCheckDataDisplayRow("Sex", seal.sex)

        // TISSUE SAMPLED
        WedCheckDataDisplayRow("Tissue Taken", seal.tissueSampled)

        // LAST SEEN
        WedCheckDataDisplayRow(
            "Last Seen",
            if (seal.lastSeenSeason == 0) "" else seal.lastSeenSeason.toString()
        )

        // PREVIOUS PUPS
        WedCheckDataDisplayRow("Previous Pups", seal.previousPups)

        // MASS PUPS
        WedCheckDataDisplayRow("Mass Pups", seal.massPups)

        // SWIM PUPS
        WedCheckDataDisplayRow("Swim Pups", seal.swimPups)

        // PHOTO YEARS
        WedCheckDataDisplayRow("Photo Years", seal.photoYears)

        // COMMENTS
        WedCheckDataDisplayRow("Comments", seal.comment)
    }
}
