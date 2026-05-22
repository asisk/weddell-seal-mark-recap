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
    FileStatus.ERROR -> Color(0xFFD90101)
    FileStatus.IDLE -> Color.Gray
    FileStatus.LOADING -> Color(0xFF5884fa)
    FileStatus.SUCCESS -> Color(0xFF0DBE0D)
}

@DrawableRes
fun FileStatus.iconRes(): Int = when (this) {
    FileStatus.ERROR -> R.drawable.ic_error_outline
    FileStatus.IDLE -> R.drawable.ic_pending
    FileStatus.LOADING -> R.drawable.ic_progress_activity
    FileStatus.SUCCESS -> R.drawable.ic_check_circle
}