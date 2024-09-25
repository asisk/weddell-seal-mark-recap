package weddellseal.markrecap.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileUploadDao {
    @Insert
    suspend fun insertFileUpload(fileUpload: FileUploadEntity): Long // Returns the ID of the inserted file

    @Query("SELECT * FROM fileUploads WHERE id = :fileId")
    suspend fun getFileUploadById(fileId: Long): FileUploadEntity

    @Query("SELECT * FROM fileUploads ORDER BY id DESC")
    fun getAllFileUploads(): Flow<List<FileUploadEntity>>

    @Query("UPDATE fileUploads SET status = :status WHERE id = :id")
    suspend fun updateFileUploadStatus(id: Long, status: String)

}