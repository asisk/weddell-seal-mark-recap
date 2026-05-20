package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal

@Composable
fun ValidationBanner(seal: Seal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFFE30707), RoundedCornerShape(4.dp))
    ) {
        seal.validationErrors.forEach { error ->
            Text(
                modifier = Modifier.padding(8.dp),
                text = error,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
