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
fun RecentFilesList(recentFiles: List<FileUploadEntity>) {
    if (recentFiles.isEmpty()) {
        Text("No imports, exports, or archives yet.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = true
        ) {
            items(recentFiles) { file ->
                RecentFilesItem(file = file)
            }
        }
    }
}