package weddellseal.markrecap.ui.tagretag

import androidx.compose.runtime.Composable
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.frameworks.room.observations.ObservationRecord

data class TabItem(
    val title: String,
    val seal: Seal,
    val content: @Composable () -> Unit
)


data class ObservationTabItem(
    val title: String,
    val seal: ObservationRecord,
    val content: @Composable () -> Unit
)