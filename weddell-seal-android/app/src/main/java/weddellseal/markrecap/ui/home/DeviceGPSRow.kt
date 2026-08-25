package weddellseal.markrecap.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.R
import weddellseal.markrecap.domain.location.data.toLocationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceGPSRow(
    viewModel: HomeViewModel,
    locationGranted: Boolean,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val location by viewModel.currentLocation.collectAsState()
    val hasFix = location?.coordinates?.longitude != null && location?.coordinates?.latitude != null

    Row(
        modifier = modifier.then(
            if (!locationGranted) {
                Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = "Enable location",
                    onClick = onEnableLocation,
                )
            } else {
                Modifier
            }
        ),
        horizontalArrangement = Arrangement.Start,
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
            if (hasFix) {
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
                    contentDescription = if (locationGranted) null else "Location off",
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
                    text = when {
                        !locationGranted -> "Location off"
                        hasFix -> location!!.toLocationString()
                        else -> "Locating..."
                    },
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
