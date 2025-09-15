package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.ui.tagretag.PupWeightOutlinedTextField


@Composable
fun PupWeightSection(
    seal: Seal,
    onEnterWeight: (Boolean) -> Unit,
    onWeightCleared: () -> Unit,
    onWeightCommitted: (Int) -> Unit,
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

        // ENTER WEIGHT SWITCH
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Enter Weight",
                style = MaterialTheme.typography.titleLarge,
            )

            Switch(
                checked = seal.weightTaken,
                onCheckedChange = { onEnterWeight(it) }
            )
        }

        // PUP WEIGHT ENTRY FIELD
        if (seal.weightTaken) {

            PupWeightOutlinedTextField(
                value = seal.weight.toString(),
                onFocusChange = { isFocused, lastValue ->
                    val number: Int? = lastValue.toIntOrNull()
                    if (number != null) {
                        if (!isFocused) onWeightCommitted(number)
                    }
                },
                onClearValueDo = { onWeightCleared() }
            )
        }
    }
}