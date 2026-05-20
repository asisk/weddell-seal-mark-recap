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
import weddellseal.markrecap.domain.tagretag.data.SealAgeClass
import weddellseal.markrecap.domain.tagretag.data.SealType
import weddellseal.markrecap.ui.tagretag.SegmentedButtonGroup

// ---- Reused constants (don’t rebuild every recomposition) ----
private val AGE_OPTIONS = SealAgeClass.values().filter { it != SealAgeClass.UNKNOWN }.map { it.description }

@Composable
fun AgeSection(
    isEditMode: Boolean,
    seal: Seal,
    onSelectAge: (SealAgeClass) -> Unit,
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
        Text("Age", style = MaterialTheme.typography.titleLarge)

        if (isEditMode && seal.sealType == SealType.PRIMARY && seal.hasPup) {

            Text(seal.ageClass.description, style = MaterialTheme.typography.titleLarge)

        } else if (seal.sealType != SealType.PRIMARY) {

            Text(SealAgeClass.PUP.description, style = MaterialTheme.typography.titleLarge)

        } else {

            SegmentedButtonGroup(
                options = AGE_OPTIONS,
                selectedOption = seal.ageClass.description,
                onOptionSelected = { onSelectAge(SealAgeClass.fromSelection(it)) }
            )
        }
    }
}