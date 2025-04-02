package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.data.FailedRow
import weddellseal.markrecap.models.HomeViewModel.UploadCardState
import weddellseal.markrecap.models.HomeViewModel.UploadStatus

@Composable
fun UploadCard(
    state: UploadCardState,
//    modifier: Modifier = Modifier,
//    title: String,
//    onUpload: () -> Unit,
//    onDelete: (() -> Unit)? = null, // Optional delete action
//    isLoading: () -> Boolean,
//    isLoaded: () -> Boolean,
//    totalRows: () -> Int,
//    failedRows: () -> List<FailedRow>,
//    fileName: () -> String
) {
    val background = when (state.status) {
        is UploadStatus.Success -> Color(0xFFD0F0E0)
        is UploadStatus.Error -> Color(0xFFFFE0E0)
        else -> Color(0xFFE3F2FD)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(400.dp)
//            .fillMaxWidth()
//            .height(240.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.fileType,
                    )
                    if (!state.lastFilename.isNullOrEmpty()) {
                        Text(
                            text = "Last: ${state.lastFilename}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = state.onUploadClick,
                    modifier = Modifier
                        .padding(start = 16.dp)
                ) {
                    Text("Upload")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.status.message,
                color = state.status.color,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
