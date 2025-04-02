package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.HomeViewModel.UploadCardState
import weddellseal.markrecap.models.HomeViewModel.UploadStatus

@Composable
fun DashboardScreen() {
    Row(Modifier.fillMaxSize()) {
        SidebarNavigation(selected = "Upload Data File", onNavigate = { /* handle nav */ })

        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4F4))
                .padding(24.dp)
        ) {
            Text("Manage Data Files", style = MaterialTheme.typography.headlineSmall)

            Row {
                UploadCard(
                    state = UploadCardState(
                        fileType = "WedCheck File",
                        status = UploadStatus.Success,
                        onUploadClick = { /* handle upload */ }
                    )
                )
                UploadCard(
                    state = UploadCardState(
                        fileType = "Observer Initials",
                        status = UploadStatus.Error("[error]"),
                        onUploadClick = { }
                    )
                )
                UploadCard(
                    state = UploadCardState(
                        fileType = "Seal Colony Locations",
                        status = UploadStatus.Success,
                        onUploadClick = { }
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Last Uploaded", style = MaterialTheme.typography.titleMedium)

            LastUploadedFileRow("data_file.csv", "Apr 22, 2024", "1.2 MB")
        }
    }
}
