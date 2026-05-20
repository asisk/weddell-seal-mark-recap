package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.ui.utils.getDeviceName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceIDRow() {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 30.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var deviceName by remember { mutableStateOf("") }
        deviceName = getDeviceName(context)
        Column(
            modifier = Modifier.fillMaxWidth(.45f)
        ) {
            Text(
                text = "Device Name",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
