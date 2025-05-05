package weddellseal.markrecap.ui.admin.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import weddellseal.markrecap.frameworks.room.files.FileState
import kotlin.Unit

@Composable
fun ExportDialog(
    onDismissRequest: () -> Unit,
    fileState: FileState,
    onConfirmArchive: () -> Unit,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "Records exported: ${fileState.recordCount}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Would you like to archive the current observations?",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color.LightGray,
                        onClick = { onDismissRequest() },
                        icon = { // no icon
                        },
                        text = {
                            Text(
                                "Not Now",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )

                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color.LightGray,
                        onClick = { onConfirmArchive() },
                        icon = { // no icon
                        },
                        text = {
                            Text(
                                "Confirm",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                }
            }
        }
    }
}