package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.RetagReason
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.ui.tagretag.RetagReasonDropDown

@Composable
fun RetagReasonSection(
    seal: Seal,
    onSelectReason: (RetagReason) -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Reason for Retag", style = MaterialTheme.typography.titleLarge)

        RetagReasonDropDown(
            selected = seal.reasonForRetag,
            onSelected = { onSelectReason(it) }
        )
    }
}