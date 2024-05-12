package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WedCheckDataDisplayRow(label: String, value: String) {
    Row(
    modifier = Modifier
    .fillMaxWidth(.7f)
    .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //AGE
            Text(text = label)
            Text(text = value)
        }
    }
}