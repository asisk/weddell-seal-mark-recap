package weddellseal.markrecap.ui.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.models.AdminViewModel
import weddellseal.markrecap.ui.file.upload.LastFilesUploadedCard

@Composable
fun DashboardScreen(
    adminViewModel: AdminViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(bottom = 20.dp),
            text = "Administration",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 36.sp,
        )
    }

    LastFilesUploadedCard(adminViewModel.successfulUploads.collectAsState().value)
}