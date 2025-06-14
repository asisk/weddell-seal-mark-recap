package weddellseal.markrecap.ui.tagretag

import androidx.compose.runtime.Composable
import weddellseal.markrecap.domain.tagretag.data.Seal

enum class SealType(val label: String) {
    PRIMARY("primary"),
    PUPONE("pupOne"),
    PUPTWO("pupTwo")
}

data class TabItem(
    val title: String,
    val seal: Seal,
    val content: @Composable () -> Unit
)