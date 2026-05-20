package weddellseal.markrecap.domain.files.data

import org.junit.Assert.assertEquals
import org.junit.Test
import weddellseal.markrecap.ui.admin.FileStatus
import weddellseal.markrecap.ui.admin.FileType

class FileTypeConverterTest {

    private val converter = FileTypeConverter()

    @Test
    fun roundTrip_allFileTypes() {
        FileType.values().forEach { type ->
            assertEquals(type, converter.toType(converter.fromType(type)))
        }
    }
}

class FileStatusConverterTest {

    private val converter = FileStatusConverter()

    @Test
    fun roundTrip_allStatuses() {
        FileStatus.values().forEach { status ->
            assertEquals(status, converter.toStatus(converter.fromStatus(status)))
        }
    }
}
