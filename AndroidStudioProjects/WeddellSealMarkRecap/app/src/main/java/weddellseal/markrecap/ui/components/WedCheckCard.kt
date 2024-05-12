package weddellseal.markrecap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.models.AddObservationLogViewModel

@Composable
fun WedCheckCard(
    viewModel: AddObservationLogViewModel,
    seal: AddObservationLogViewModel.Seal
) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        // NOTEBOOK DISPLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // SEAL CARD HEADING/NOTEBOOK STRING
            var headingStr = ""
            if (seal.age.isNotEmpty()) {
                headingStr = seal.age + " :  "
            }
            Text(
                headingStr + seal.notebookDataString,
                style = MaterialTheme.typography.titleLarge
            )
        }
        //AGE
        WedCheckDataDisplayRow("Age", seal.age)

        // SEX
        WedCheckDataDisplayRow("Sex", seal.sex)

        // TISSUE SAMPLED
        WedCheckDataDisplayRow("Tissue Taken", seal.tissueTaken.toString())

        //TAG ID
        WedCheckDataDisplayRow( "Tag", seal.tagId)

        // COMMENTS
        WedCheckDataDisplayRow("Comments",seal.comment)
    }
}
