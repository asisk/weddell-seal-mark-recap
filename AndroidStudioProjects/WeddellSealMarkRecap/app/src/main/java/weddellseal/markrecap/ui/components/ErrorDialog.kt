package weddellseal.markrecap.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


@Composable
fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "Error")
        },
        text = {
            Text(text = errorMessage)
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text(text = "Dismiss")
            }
        }
    )
}