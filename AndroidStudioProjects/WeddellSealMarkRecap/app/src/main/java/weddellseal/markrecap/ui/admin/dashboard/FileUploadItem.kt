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
import weddellseal.markrecap.frameworks.room.files.color
import weddellseal.markrecap.frameworks.room.files.icon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileUploadItem(fileUpload: FileUploadEntity) {
    var text by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableStateOf(Icons.Default.Pending) }

    LaunchedEffect(fileUpload.status) {
        text = fileUpload.status.message
        statusColor = fileUpload.status.color()
        statusIcon = fileUpload.status.icon()
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
                fileUpload.filename,
                style = MaterialTheme.typography.titleLarge,
            )


        }

        // datetime of upload
        Text(
            "${
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.US
                ).format(Date(fileUpload.createdAt))
            }",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp)
        )

        // status icon
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier
                .size(48.dp)
                .padding(start = 12.dp, end = 8.dp, top = 2.dp)
        )
    }
}