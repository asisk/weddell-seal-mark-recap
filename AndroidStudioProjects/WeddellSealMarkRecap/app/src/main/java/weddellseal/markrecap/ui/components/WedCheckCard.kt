package weddellseal.markrecap.ui.components

import androidx.compose.runtime.Composable
import weddellseal.markrecap.models.WedCheckViewModel

@Composable
fun WedCheckCard(
    seal: WedCheckViewModel.WedCheckSeal
) {
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
