package weddellseal.markrecap.ui.admin.dashboard

import androidx.annotation.DrawableRes
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.admin.FileAction

data class FileHistoryAppearance(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
)

/**
 * Import rows keep the upload icon. Exports and archives use the same icons as
 * the Export and Archive screens so a glance at Recent Files shows which
 * step happened.
 */
fun fileHistoryAppearance(fileAction: String): FileHistoryAppearance = when (fileAction) {
    FileAction.EXPORT.name, FileAction.DOWNLOAD.name -> FileHistoryAppearance(
        iconRes = R.drawable.ic_file_download_outlined,
        contentDescription = "Exported",
    )
    FileAction.ARCHIVE.name -> FileHistoryAppearance(
        iconRes = R.drawable.ic_archive_outlined,
        contentDescription = "Archived",
    )
    else -> FileHistoryAppearance(
        iconRes = R.drawable.ic_upload_file_outlined,
        contentDescription = "Imported",
    )
}
