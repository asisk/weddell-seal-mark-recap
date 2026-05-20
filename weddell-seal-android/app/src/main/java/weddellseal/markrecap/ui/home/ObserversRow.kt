package weddellseal.markrecap.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObserversRow(
    viewModel: HomeViewModel
) {
    val metadata by viewModel.metadata.collectAsState()
    val observerOptions by viewModel.observersList.collectAsState() // Collecting the list of observers

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Observer Initials",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.width(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            ObserversDropDown(
                label = "Selected Observers",
                allOptions = observerOptions,
                selectedOptions = metadata.selectedObservers,
                onSelectionChanged = { updatedItems ->
                    viewModel.updateObserversSelection(
                        updatedItems
                    )
                },
            )
        }
    }
}
