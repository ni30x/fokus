package nwd.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indexed_folders")
data class IndexedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastIndexedAt: Long = System.currentTimeMillis(),
    val documentCount: Int = 0
)
