package nwd.fokuslauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nwd.fokuslauncher.data.database.entity.IndexedDocumentEntity
import nwd.fokuslauncher.data.database.entity.IndexedFolderEntity

@Dao
interface IndexedDocumentDao {

    @Query("SELECT * FROM indexed_documents WHERE displayName LIKE '%' || :query || '%' ORDER BY lastModified DESC LIMIT :limit")
    suspend fun searchDocuments(query: String, limit: Int = 10): List<IndexedDocumentEntity>

    @Query("SELECT * FROM indexed_folders ORDER BY addedAt DESC")
    fun getAllFoldersFlow(): Flow<List<IndexedFolderEntity>>

    @Query("SELECT * FROM indexed_folders ORDER BY addedAt DESC")
    suspend fun getAllFolders(): List<IndexedFolderEntity>

    @Query("SELECT * FROM indexed_folders WHERE treeUri = :treeUri LIMIT 1")
    suspend fun getFolderByUri(treeUri: String): IndexedFolderEntity?

    @Query("SELECT * FROM indexed_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): IndexedFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: IndexedFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: IndexedFolderEntity)

    @Query("DELETE FROM indexed_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Query("DELETE FROM indexed_documents WHERE folderId = :folderId")
    suspend fun deleteDocumentsByFolderId(folderId: Long)

    @Query("DELETE FROM indexed_documents WHERE treeUri = :treeUri")
    suspend fun deleteDocumentsByTreeUri(treeUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<IndexedDocumentEntity>)

    @Query("SELECT COUNT(*) FROM indexed_documents")
    fun getTotalDocumentCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM indexed_documents")
    suspend fun getTotalDocumentCount(): Int

    @Transaction
    suspend fun deleteFolderAndDocuments(folderId: Long) {
        deleteDocumentsByFolderId(folderId)
        deleteFolderById(folderId)
    }

    @Transaction
    suspend fun replaceDocumentsForFolder(folderId: Long, documents: List<IndexedDocumentEntity>, updatedFolder: IndexedFolderEntity) {
        deleteDocumentsByFolderId(folderId)
        if (documents.isNotEmpty()) {
            insertDocuments(documents)
        }
        updateFolder(updatedFolder)
    }
}
