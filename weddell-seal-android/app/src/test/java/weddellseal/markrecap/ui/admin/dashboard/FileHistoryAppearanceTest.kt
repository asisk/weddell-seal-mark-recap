package weddellseal.markrecap.ui.admin.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import weddellseal.markrecap.R
import weddellseal.markrecap.ui.admin.FileAction

class FileHistoryAppearanceTest {

    @Test
    fun exportUsesTheExportIcon() {
        val appearance = fileHistoryAppearance(FileAction.EXPORT.name)
        assertEquals(R.drawable.ic_file_download_outlined, appearance.iconRes)
        assertEquals("Exported", appearance.contentDescription)
    }

    @Test
    fun archiveUsesTheArchiveIcon() {
        val appearance = fileHistoryAppearance(FileAction.ARCHIVE.name)
        assertEquals(R.drawable.ic_archive_outlined, appearance.iconRes)
        assertEquals("Archived", appearance.contentDescription)
    }

    @Test
    fun uploadUsesTheImportIcon() {
        val appearance = fileHistoryAppearance(FileAction.UPLOAD.name)
        assertEquals(R.drawable.ic_upload_file_outlined, appearance.iconRes)
        assertEquals("Imported", appearance.contentDescription)
    }
}
