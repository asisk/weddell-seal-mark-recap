package weddellseal.markrecap.ui.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SummaryListItem(headText: String, trailText: String) {
    ListItem(
        headlineContent = {
            Text(
                headText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        trailingContent = {
            Text(
                trailText,
                style = MaterialTheme.typography.titleLarge
            )
        }
    )
}