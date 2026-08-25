package weddellseal.markrecap.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/**
 * Scaffold body padding plus IME, without double-counting insets Scaffold already applied.
 * API 35/36 edge-to-edge no longer resizes the window for the keyboard.
 */
fun Modifier.scaffoldContentInsets(innerPadding: PaddingValues): Modifier =
    this
        .padding(innerPadding)
        .consumeWindowInsets(innerPadding)
        .imePadding()
