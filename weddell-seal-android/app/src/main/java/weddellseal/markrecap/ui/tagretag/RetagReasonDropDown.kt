package weddellseal.markrecap.ui.tagretag

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.toLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetagReasonDropDown(
    selected: RetagReason,
    onSelected: (RetagReason) -> Unit
) {
    val retagReasons = RetagReason.values()
        .filter { it != RetagReason.UNKNOWN }
        .map { it.toLabel() }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            value = selected.toLabel(),
            onValueChange = {},
            label = { Text("Retag Reason") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).widthIn(max = 200.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            retagReasons.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(RetagReason.fromLabel(label))
                        expanded = false
                    }
                )
            }
        }
    }
}
