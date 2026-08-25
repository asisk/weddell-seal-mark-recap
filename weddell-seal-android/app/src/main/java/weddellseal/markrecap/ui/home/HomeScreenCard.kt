package weddellseal.markrecap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenCard(
    viewModel: HomeViewModel,
    isLandscape: Boolean = false,
) {
    val metadata by viewModel.metadata.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.height(350.dp))
            .padding(horizontal = if (isLandscape) 24.dp else 30.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (isLandscape) 12.dp else 0.dp,
                    bottom = if (isLandscape) 12.dp else 0.dp
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ObserversRow(viewModel)

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 20.dp))

            ColonyRow(viewModel)

            if (metadata.selectedColony?.location == "Other") {
                CoordinateFieldEntryRow(viewModel)
            }
        }
    }
}
