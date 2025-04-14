package weddellseal.markrecap.ui.file.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import weddellseal.markrecap.models.ObserversViewModel
import weddellseal.markrecap.models.SealColoniesViewModel
import weddellseal.markrecap.models.WedCheckViewModel

@Composable
fun UploadDataFileScreen(
    wedCheckViewModel: WedCheckViewModel,
    sealColoniesViewModel: SealColoniesViewModel,
    observersViewModel: ObserversViewModel,
) {
    val wedCheckUploadFileState by wedCheckViewModel.wedCheckUploadState.collectAsState()
    val sealColonyFileState by sealColoniesViewModel.fileState.collectAsState()
    val observersFileState by observersViewModel.fileState.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.padding(bottom = 20.dp),
                text = "Manage Uploads",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 36.sp,
            )
        }
        UploadCard(state = wedCheckUploadFileState)
        UploadCard(state = observersFileState)
        UploadCard(state = sealColonyFileState)
    }
}
