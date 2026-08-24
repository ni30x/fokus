package nwd.fokuslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import nwd.fokuslauncher.data.database.dao.AppDao
import nwd.fokuslauncher.data.database.dao.IndexedDocumentDao
import nwd.fokuslauncher.data.database.entity.AppCategoryEntity
import nwd.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import nwd.fokuslauncher.data.database.entity.HiddenAppEntity
import nwd.fokuslauncher.data.database.entity.IndexedDocumentEntity
import nwd.fokuslauncher.data.database.entity.IndexedFolderEntity
import nwd.fokuslauncher.data.database.entity.RenamedAppEntity
import nwd.fokuslauncher.data.database.entity.SuppressedCategoryDefinitionEntity

@Database(
    entities = [
        HiddenAppEntity::class,
        RenamedAppEntity::class,
        AppCategoryEntity::class,
        AppCategoryDefinitionEntity::class,
        SuppressedCategoryDefinitionEntity::class,
        IndexedFolderEntity::class,
        IndexedDocumentEntity::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun indexedDocumentDao(): IndexedDocumentDao

    companion object {
        @Volatile
        var instance: AppDatabase? = null
    }
}
