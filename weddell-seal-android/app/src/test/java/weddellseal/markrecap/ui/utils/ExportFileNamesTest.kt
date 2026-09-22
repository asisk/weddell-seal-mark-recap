package weddellseal.markrecap.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFileNamesTest {

    private val fileDate = "20260819_155900"

    @Test
    fun suggestedName_includesTabletLetter() {
        assertEquals(
            "observations_H_20260819_155900.csv",
            observationExportSuggestedName("H", fileDate, allRecords = false),
        )
    }

    @Test
    fun suggestedName_allRecordsPrefix() {
        assertEquals(
            "all_observations_H_20260819_155900.csv",
            observationExportSuggestedName("H", fileDate, allRecords = true),
        )
    }

    @Test
    fun sanitize_replacesSpacesAndPunctuation() {
        assertEquals("Tablet_H", sanitizeDeviceNameForFilename("Tablet H"))
        assertEquals("H", sanitizeDeviceNameForFilename("H!"))
        assertEquals("Galaxy_Tab_A", sanitizeDeviceNameForFilename("Galaxy Tab A"))
    }

    @Test
    fun sanitize_collapsesRepeatedSeparators() {
        assertEquals("A_B", sanitizeDeviceNameForFilename("A  B"))
        assertEquals("A_B", sanitizeDeviceNameForFilename("A!!B"))
    }

    @Test
    fun sanitize_keepsLettersDigitsDotUnderscoreHyphen() {
        assertEquals("Tab-H_2.0", sanitizeDeviceNameForFilename("Tab-H_2.0"))
    }

    @Test
    fun sanitize_fallsBackToTabletWhenEmptyOrUnknown() {
        assertEquals("tablet", sanitizeDeviceNameForFilename(""))
        assertEquals("tablet", sanitizeDeviceNameForFilename("   "))
        assertEquals("tablet", sanitizeDeviceNameForFilename("Unknown Device"))
        assertEquals("tablet", sanitizeDeviceNameForFilename("unknown device"))
        assertEquals("tablet", sanitizeDeviceNameForFilename("???"))
    }

    @Test
    fun suggestedName_usesTabletFallback() {
        assertEquals(
            "observations_tablet_20260819_155900.csv",
            observationExportSuggestedName("Unknown Device", fileDate, allRecords = false),
        )
        assertEquals(
            "all_observations_tablet_20260819_155900.csv",
            observationExportSuggestedName("", fileDate, allRecords = true),
        )
    }
}
