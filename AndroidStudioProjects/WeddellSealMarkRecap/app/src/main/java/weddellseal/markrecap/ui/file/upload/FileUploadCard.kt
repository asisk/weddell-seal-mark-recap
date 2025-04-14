package weddellseal.markrecap.ui.file.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.files.FileState
import weddellseal.markrecap.frameworks.room.files.color
import weddellseal.markrecap.frameworks.room.files.icon
import weddellseal.markrecap.ui.file.FileStatus

@Composable
fun UploadCard(
    state: FileState,
) {

    var errMessage by remember { mutableStateOf("") }
    var lastFilename by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableStateOf(Icons.Default.Pending) }

    LaunchedEffect(state.status) {
        errMessage = state.errorMessage.toString()
        lastFilename = state.lastFilename.toString()
        statusColor = state.status.color()
        statusIcon = state.status.icon()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(400.dp)
//            .fillMaxWidth()
//            .height(240.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.fileType,
                    )
                }

                Button(
                    onClick = state.onUploadClick,
                    modifier = Modifier
                        .padding(start = 16.dp)
                ) {
                    Text("Upload")
                }
            }


            // Upload status
            if (state.status == FileStatus.ERROR || state.status == FileStatus.SUCCESS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(start = 12.dp, end = 8.dp, top = 2.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = errMessage,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = lastFilename,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

//@Composable
//fun FailedRowsDisplay(failedRows: List<FailedRow>) {
//    if (failedRows.isNotEmpty()) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(
//                text = "Failed Rows",
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.error
//            )
//
//            failedRows.forEach { row ->
//                Text(
//                    text = "Row ${row.rowNumber}: ${row.errorMessage}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onError
//                )
//            }
//        }
//    } else {
//        Text(
//            text = "No failed rows found.",
//            modifier = Modifier.padding(16.dp),
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.primary
//        )
//    }
//}
