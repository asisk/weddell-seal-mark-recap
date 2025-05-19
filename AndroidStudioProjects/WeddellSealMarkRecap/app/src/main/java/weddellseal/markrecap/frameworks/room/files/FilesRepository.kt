package weddellseal.markrecap.frameworks.room.files

import kotlinx.coroutines.flow.Flow
import weddellseal.markrecap.ui.admin.FileAction
import weddellseal.markrecap.ui.admin.FileStatus

class FilesRepository(
    private val fileUploadDao: FileUploadDao
) {

    // Return flow of file uploads from the DAO
    val fileUploads: Flow<List<FileUploadEntity>> = fileUploadDao.getAllFileUploads()
    val successfulUploads: Flow<List<FileUploadEntity>> = fileUploadDao.getSuccessfulFileUploads()

    fun FileUploadEntity.toFileState(): FileState {
        return FileState(
            fileType = fileType.name,
            action = FileAction.valueOf(fileAction),
            status = status,
            message = statusMessage,
            onUploadClick = {
                //this value is intentionally not populated in the database
            },
            onExportClick = {
                //this value is intentionally not populated in the database
            },
            lastUploadFilename = filename
        )
    }

    // Insert file upload entry and get the fileUploadId
    suspend fun insertFileUpload(fileUpload: FileUploadEntity): Long {
        return fileUploadDao.insertFileUpload(fileUpload)
    }

    suspend fun updateFileUploadStatus(fileUploadId: Long, status: FileStatus, recordCount: Int, statusMsg: String) {
        fileUploadDao.updateFileUploadStatus(fileUploadId, status, recordCount, statusMsg)
    }
}