package weddellseal.markrecap.ui.tagretag.sealcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.domain.tagretag.data.Seal
import weddellseal.markrecap.ui.tagretag.CommentField

@Composable
fun TissueCommentSection(
    seal: Seal,
    onSelectTissue: (Boolean) -> Unit,
    onClearComment: () -> Unit,
    onCommentChanged: (String) -> Unit = {},
    onCommentCommitted: (String) -> Unit,
    modifier: Modifier,
    fieldKey: Any = 0,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // TISSUE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tissue",
                style = MaterialTheme.typography.titleLarge,
            )

            Checkbox(
                checked = seal.tissueTaken,
                onCheckedChange = { onSelectTissue(it) }
            )
        }

        Spacer(modifier = Modifier.width(30.dp))

        // COMMENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            CommentField(
                value = seal.comment,
                onClearValueDo = { onClearComment() },
                onValueChange = onCommentChanged,
                onFocusChange = { isFocused, lastValue ->
                    if (!isFocused) onCommentCommitted(lastValue.trim())
                },
                fieldKey = fieldKey,
            )

            Text(
                text = "Comment field allows letters, numbers, and certain special characters.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
