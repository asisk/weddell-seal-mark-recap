package weddellseal.markrecap.ui.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.ui.utils.formatFileUploadedDateTime

@Composable
fun FileImportItem(successfulUpload: FileUploadEntity) {
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
                painter = painterResource(R.drawable.ic_upload_file_outlined),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = successfulUpload.filename,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = formatFileUploadedDateTime(successfulUpload.createdAt)
                    + "\n" + "Records: "
                    + successfulUpload.recordCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp)
        )
    }
}