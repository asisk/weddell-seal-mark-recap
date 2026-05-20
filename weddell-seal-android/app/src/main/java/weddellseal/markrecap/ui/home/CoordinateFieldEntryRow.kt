package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoordinateFieldEntryRow(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "-77.",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(6.dp))

        // LATITUDE
        CoordinatesTextField(
            value = uiState.latitudeDecimals,
            onFocusChange = { isFocused, lastValue ->
                if (!isFocused) viewModel.updateOtherColonyLatitude(lastValue)
            },
            onClearValueDo = { viewModel.clearOtherColonyLatitude() }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "166.",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(6.dp))

        // LONGITUDE
        CoordinatesTextField(
            value = uiState.longitudeDecimals,
            onFocusChange = { isFocused, lastValue ->
                if (!isFocused) viewModel.updateOtherColonyLongitude(lastValue)
            },
            onClearValueDo = { viewModel.clearOtherColonyLongitude() }
        )
    }
}