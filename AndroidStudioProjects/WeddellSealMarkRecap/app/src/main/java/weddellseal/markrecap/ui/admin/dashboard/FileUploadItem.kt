package weddellseal.markrecap.ui.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.UploadFile
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
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.domain.files.data.color
import weddellseal.markrecap.domain.files.data.icon
import weddellseal.markrecap.ui.utils.formatFileUploadedDateTime

@Composable
fun FileUploadItem(successfulUpload: FileUploadEntity) {
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableStateOf(Icons.Default.Pending) }

    LaunchedEffect(successfulUpload.status) {
        statusColor = successfulUpload.status.color()
        statusIcon = successfulUpload.status.icon()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
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
                    .padding(end = 8.dp)
            )
            // filename
            Text(
                text = successfulUpload.filename,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // datetime of upload
        Text(
            text = formatFileUploadedDateTime(successfulUpload.createdAt)
                    + "\n" + "Rows: "
                    + successfulUpload.recordCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp)
        )
    }
}