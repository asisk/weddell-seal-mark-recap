package weddellseal.markrecap.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LocationPermissionView(
    onNextClick: () -> Unit = {}
) {
    var requestPermissions by remember { mutableStateOf(false) }

    if (requestPermissions) {
        RequestPermissions(LocalContext.current.locationPermissions()) {
            onNextClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        Icon(
            Icons.Filled.LocationOn,
            contentDescription = "permission icon",
            modifier = Modifier.size(100.dp)
        )

        Text(text = "Enable Location", style = MaterialTheme.typography.headlineLarge)

        Text(
            text = "By allowing location services, we can get you the local information you need.",
            modifier = Modifier.padding(horizontal = 20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1.0f))

        ExtendedFloatingActionButton(
            modifier = Modifier.padding(16.dp),
            containerColor = Color.LightGray,
            onClick = { requestPermissions = true },
            icon = { // no icon
            },
            text = {
                Text(
                    "Grant Location Permissions",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        )
    }
}