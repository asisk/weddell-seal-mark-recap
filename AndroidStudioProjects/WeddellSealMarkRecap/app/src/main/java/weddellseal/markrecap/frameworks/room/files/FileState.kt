package weddellseal.markrecap.frameworks.room.files

import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pending
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import weddellseal.markrecap.ui.file.FileAction
import weddellseal.markrecap.ui.file.FileStatus

data class FileState(
    val fileType: String,
    val action: FileAction,
    val status: FileStatus,
    val errorMessage: String? = null,
    val onUploadClick: () -> Unit,
    val onDownloadClick: () -> Unit,
    val downloadFilename: String? = null,
    val lastFilename: String? = null,
    val recordCount: Int = 0
)

fun FileStatus.color(): Color = when (this) {
    FileStatus.IDLE -> Color.Gray
    FileStatus.SUCCESS -> Color(0xFF0DBE0D)
    FileStatus.ERROR -> Color(0xFFD90101)
}

fun FileStatus.icon(): ImageVector = when (this) {
    FileStatus.ERROR -> Icons.Default.ErrorOutline
    FileStatus.SUCCESS -> Icons.Default.CheckCircle
    FileStatus.IDLE -> Icons.Default.Pending
}