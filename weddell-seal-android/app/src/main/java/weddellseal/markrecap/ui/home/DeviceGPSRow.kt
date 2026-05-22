package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.location.data.toLocationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceGPSRow(
    viewModel: HomeViewModel
) {
    val location by viewModel.currentLocation.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Device GPS",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.width(40.dp))

        Column {
            if (location?.coordinates?.longitude != null && location?.coordinates?.latitude != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_location_on),
                    contentDescription = null,
                    tint = Color(0xFF1D9C06),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(36.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_location_off),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(36.dp),
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f)  // take the remaining space
        ) {
            Column {
                Text(
                    text = location?.toLocationString() ?: "Locating...",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                location?.updatedDate?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
            }
        }
//                                Text(
//                                    text = if (viewModel.hasPreciseLocation(context))
//                                        "Using precise location"
//                                    else
//                                        "Using approximate location"
//                                )
    }
}
