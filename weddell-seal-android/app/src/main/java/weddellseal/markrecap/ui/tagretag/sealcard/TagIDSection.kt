package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.tagretag.SingleSelectTagAlphaButtonGroup
import weddellseal.markrecap.ui.tagretag.TagIDOutlinedTextField

// ---- Reused constants (don’t rebuild every recomposition) ----
private val TAG_ALPHAS = listOf("A", "C", "D")

@Composable
 fun TagIdSection(
    label: String,
    number: String,
    alpha: String,
    onClear: () -> Unit,
    onNumberChanged: (String) -> Unit = {},
    onNumberCommitted: (String) -> Unit,
    onAlphaSelected: (String) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(.4f)
                .padding(end = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                TagIDOutlinedTextField(
                    value = number,
                    labelText = "3 or 4 Digit Tag Number",
                    placeholderText = "Enter Tag Number",
                    keyboardType = KeyboardType.Number,
                    onClearValueDo = onClear,
                    onValueChange = onNumberChanged,
                    onFocusChange = { isFocused, lastValue ->
                        if (!isFocused) onNumberCommitted(lastValue)
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(.4f)
                .padding(start = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                SingleSelectTagAlphaButtonGroup(
                    TAG_ALPHAS,
                    alpha
                ) { onAlphaSelected(it) }
            }
        }
    }
}
