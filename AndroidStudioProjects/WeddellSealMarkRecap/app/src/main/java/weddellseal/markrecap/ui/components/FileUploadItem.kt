package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.FileUploadEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileUploadItem(fileUpload: FileUploadEntity) {

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

        // Upload status
        val statusColor = when (fileUpload.status.lowercase()) {
            "successful" -> Color(0xFF0dbe0d)
            "error" -> Color(0xFFd90101)
            else -> Color(0xFF5884fa)
        }
        val statusIcon = when (fileUpload.status.lowercase()) {
            "successful" -> Icons.Default.CheckCircle
            "error" -> Icons.Default.ErrorOutline
            else -> Icons.Default.Pending
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