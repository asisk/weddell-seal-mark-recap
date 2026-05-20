package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealRelatives
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.ui.tagretag.SegmentedButtonGroup

// ---- Reused constants (don’t rebuild every recomposition) ----
private val NUM_RELS_OPTIONS =
    SealRelatives.values().filter { it != SealRelatives.UNKNOWN }.map { it.label }

@Composable
fun RelativesSection(
    isEditMode: Boolean,
    seal: Seal,
    onSelectRelatives: (SealRelatives) -> Unit,
    onSelectPupPeed: (Boolean) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
    Text("# of Rels", style = MaterialTheme.typography.titleLarge)

    if (isEditMode && seal.hasPup) {

        // number of relatives is not selectable in edit mode if the primary seal has a pup
        // pups may be removed via the delete button
        Text(
            seal.numRelatives.label,
            style = MaterialTheme.typography.titleLarge
        )

    } else {

        if (seal.ageClass == SealAgeClass.PUP || seal.ageClass == SealAgeClass.YEARLING) {

            // when the primary seal is a pup or yearling, there can be no other relatives
            Text(
                seal.numRelatives.label,
                style = MaterialTheme.typography.titleLarge
            )

        } else if (seal.sealType == SealType.PRIMARY && seal.sex == SealSex.MALE && seal.numRelatives == SealRelatives.ZERO) {

            // when the primary seal is a male, there can be no other relatives
            Text(
                seal.numRelatives.label,
                style = MaterialTheme.typography.titleLarge
            )

        } else {

            SegmentedButtonGroup(
                options = NUM_RELS_OPTIONS,
                selectedOption = seal.numRelatives.label,
                onOptionSelected = { onSelectRelatives(SealRelatives.fromSelection(it)) }
            )
        }
    }

    Spacer(modifier = Modifier.width(30.dp))

    // PUP PEED CHECKBOX
    Box(
        modifier = Modifier.weight(1f)  // take the remaining space
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // display pup peed only for pups
            if (seal.ageClass == SealAgeClass.PUP) {
                Text(
                    text = "Pup Peed",
                    style = MaterialTheme.typography.titleLarge,
                )

                Checkbox(
                    checked = seal.pupPeed,
                    onCheckedChange = { onSelectPupPeed(it) },
                )
            }
        }
    }
}
}