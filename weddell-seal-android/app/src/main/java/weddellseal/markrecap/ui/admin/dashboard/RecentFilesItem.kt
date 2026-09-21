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
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.ui.utils.formatFileUploadedDateTime

@Composable
fun RecentFilesItem(file: FileUploadEntity) {
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
            val appearance = fileHistoryAppearance(file.fileAction)
            Icon(
                painter = painterResource(appearance.iconRes),
                contentDescription = appearance.contentDescription,
                tint = Color.Black,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = file.filename,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = formatFileUploadedDateTime(file.createdAt)
                    + "\n" + "Records: "
                    + file.recordCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp)
        )
    }
}