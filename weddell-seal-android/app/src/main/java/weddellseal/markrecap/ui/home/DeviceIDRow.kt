package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.utils.getDeviceName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceIDRow(
    modifier: Modifier = Modifier
) {
    val deviceName = getDeviceName(LocalContext.current)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Device Name",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
