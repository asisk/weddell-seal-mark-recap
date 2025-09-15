package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.domain.tagretag.data.SealSex
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.ui.tagretag.SegmentedButtonGroup

// ---- Reused constants (don’t rebuild every recomposition) ----
private val SEX_OPTIONS = SealSex.values().filter { it != SealSex.NONE }.map { it.description }

@Composable
fun SexSection(
    isEditMode: Boolean,
    seal: Seal,
    onSelectSex: (SealSex) -> Unit,
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
        Text("Sex", style = MaterialTheme.typography.titleLarge)

        if (isEditMode && seal.sealType == SealType.PRIMARY && seal.hasPup) {

            // sex is not selectable in edit mode for the primary seal
            Text(seal.ageClass.description, style = MaterialTheme.typography.titleLarge)

        } else {

            SegmentedButtonGroup(
                options = SEX_OPTIONS,
                selectedOption = seal.sex.description,
                onOptionSelected = { onSelectSex(SealSex.fromSelection(it)) }
            )
        }
    }
}