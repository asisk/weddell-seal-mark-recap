package weddellseal.markrecap.ui.admin.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.files.data.FileState
import weddellseal.markrecap.domain.files.data.color
import weddellseal.markrecap.domain.files.data.iconRes
import weddellseal.markrecap.ui.admin.FileStatus

@Composable
fun ImportCard(
    state: FileState,
) {
    var errMessage by remember { mutableStateOf("") }
    var lastFilename by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF5884fa)) }
    var statusIcon by remember { mutableIntStateOf(R.drawable.ic_pending) }

    LaunchedEffect(state.status) {
        errMessage = state.message.toString()
        lastFilename = state.lastUploadFilename.toString()
        statusColor = state.status.color()
        statusIcon = state.status.iconRes()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
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
                        painter = painterResource(R.drawable.ic_upload_file_outlined),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.fileType,
                    )
                }

                Button(
                    onClick = state.onUploadClick,
                    modifier = Modifier
                        .padding(start = 16.dp)
                ) {
                    Text("Import",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            if (state.status == FileStatus.ERROR || state.status == FileStatus.SUCCESS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(statusIcon),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(start = 12.dp, end = 8.dp, top = 2.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.status == FileStatus.SUCCESS) {
                            Text(
                                text = lastFilename + "\nTotal Rows Uploaded: ${state.recordCount}",
                                color = statusColor,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                text = errMessage,
                                color = statusColor,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}