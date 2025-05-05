package weddellseal.markrecap.ui.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity

@Composable
fun FileUploadList(fileUploads: List<FileUploadEntity>) {
    if (fileUploads.isEmpty()) {
        Text("No file uploads found.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = true
        ) {
            items(fileUploads) { file ->
                FileUploadItem(fileUpload = file)
            }
        }
    }
}