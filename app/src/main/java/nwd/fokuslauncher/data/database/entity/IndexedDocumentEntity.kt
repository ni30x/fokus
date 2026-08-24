package nwd.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indexed_documents",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["displayName"]),
        Index(value = ["documentUri"], unique = true)
    ]
)
data class IndexedDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,
    val treeUri: String,
    val documentId: String,
    val documentUri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long
)
