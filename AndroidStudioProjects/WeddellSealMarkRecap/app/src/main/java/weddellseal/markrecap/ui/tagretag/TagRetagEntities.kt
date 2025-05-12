package weddellseal.markrecap.ui.tagretag

import androidx.compose.runtime.Composable

enum class SealType(val label: String) {
    PRIMARY("primary"),
    PUPONE("pupOne"),
    PUPTWO("pupTwo")
}

data class TabItem(
    val title: String,
    val sealName: String,
    val content: @Composable () -> Unit
)