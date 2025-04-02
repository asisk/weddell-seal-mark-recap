package weddellseal.markrecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SidebarNavigation(selected: String, onNavigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(200.dp)
            .background(Color(0xFF212121))
            .padding(top = 24.dp)
    ) {
        listOf("Dashboard", "Upload Data File", "Export Observations", "Recent Files").forEach { item ->
            val isSelected = selected == item
            val bgColor = if (isSelected) Color.Gray else Color.Transparent
            Text(
                text = item,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(16.dp)
                    .clickable { onNavigate(item) }
            )
        }
    }
}
