package nwd.fokuslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import nwd.fokuslauncher.data.database.dao.AppDao
import nwd.fokuslauncher.data.database.entity.AppCategoryEntity
import nwd.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import nwd.fokuslauncher.data.database.entity.HiddenAppEntity
import nwd.fokuslauncher.data.database.entity.RenamedAppEntity
import nwd.fokuslauncher.data.database.entity.SuppressedCategoryDefinitionEntity

@Database(
    entities = [
        HiddenAppEntity::class,
        RenamedAppEntity::class,
        AppCategoryEntity::class,
        AppCategoryDefinitionEntity::class,
        SuppressedCategoryDefinitionEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        var instance: AppDatabase? = null
    }
}
