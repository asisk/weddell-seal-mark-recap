package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionChip(
    text: String,
    onDismiss: () -> Unit,
) {
    var enabled by remember { mutableStateOf(true) }

    // Handle dismissal when the chip is clicked.
    val handleDismissal = {
        onDismiss()
        enabled = !enabled
    }

    if (!enabled) return

    InputChip(
        onClick = handleDismissal,
        label = { Text(text) },
        selected = enabled,
//        avatar = {
//            Icon(
//                Icons.Filled.Person,
//                contentDescription = "Localized description",
//                Modifier.size(InputChipDefaults.AvatarSize)
//            )
//        },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Localized description",
                Modifier.size(InputChipDefaults.AvatarSize)
            )
        },
    )
}