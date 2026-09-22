package weddellseal.markrecap.ui.admin.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import weddellseal.markrecap.frameworks.room.files.FileUploadEntity
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType
import weddellseal.markrecap.ui.theme.WeddellSealMarkRecapTheme

@Preview(
    name = "Recent files",
    showBackground = true,
    widthDp = 1000,
    heightDp = 640,
)
@Composable
private fun PreviewDashboardRecentFiles() {
    WeddellSealMarkRecapTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            DashboardScreen(recentFiles = sampleRecentFiles)
        }
    }
}

private val sampleRecentFiles = listOf(
    FileUploadEntity(
        id = 3,
        fileType = FileType.ARCHIVE,
        fileAction = FileAction.ARCHIVE.name,
        filename = "Current observations",
        createdAt = 1_790_004_600_000L,
        status = FileStatus.SUCCESS,
        recordCount = 42,
    ),
    FileUploadEntity(
        id = 2,
        fileType = FileType.WEDDATACURRENT,
        fileAction = FileAction.EXPORT.name,
        filename = "observations_H_20260921_153000.csv",
        createdAt = 1_790_001_000_000L,
        status = FileStatus.SUCCESS,
        recordCount = 42,
    ),
    FileUploadEntity(
        id = 1,
        fileType = FileType.WEDCHECK,
        fileAction = FileAction.UPLOAD.name,
        filename = "wedcheck.csv",
        createdAt = 1_789_960_000_000L,
        status = FileStatus.SUCCESS,
        recordCount = 1280,
    ),
)
