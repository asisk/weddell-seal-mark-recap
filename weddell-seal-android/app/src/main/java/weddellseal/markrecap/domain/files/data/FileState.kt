package weddellseal.markrecap.domain.files.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus

data class FileState(
    val fileType: String,
    val action: FileAction,
    val status: FileStatus,
    val message: String? = null,
    val onUploadClick: () -> Unit,
    val onExportClick: () -> Unit,
    val exportFilename: String? = null,
    val lastUploadFilename: String? = null,
    val recordCount: Int = 0
)

fun FileStatus.color(): Color = when (this) {
    FileStatus.IDLE -> Color.Gray
    FileStatus.SUCCESS -> Color(0xFF0DBE0D)
    FileStatus.ERROR -> Color(0xFFD90101)
}

@DrawableRes
fun FileStatus.iconRes(): Int = when (this) {
    FileStatus.IDLE -> R.drawable.ic_pending
    FileStatus.SUCCESS -> R.drawable.ic_check_circle
    FileStatus.ERROR -> R.drawable.ic_error_outline
}
