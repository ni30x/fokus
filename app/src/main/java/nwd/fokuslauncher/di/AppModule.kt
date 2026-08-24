package nwd.fokuslauncher.di

import android.content.Context.LAUNCHER_APPS_SERVICE
import android.content.Context.USER_SERVICE
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import nwd.fokuslauncher.data.database.AppDatabase
import nwd.fokuslauncher.data.database.dao.AppDao
import nwd.fokuslauncher.data.database.dao.IndexedDocumentDao
import nwd.fokuslauncher.data.local.PreferencesManager
import nwd.fokuslauncher.data.model.appProfileKey
import nwd.fokuslauncher.data.repository.WeatherRepository
import nwd.fokuslauncher.utils.PrivateSpaceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_category_definitions` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))"
                )
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `app_category_definitions` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

    private val MIGRATION_3_TO_4_NEW_TABLES =
        listOf(
            "CREATE TABLE IF NOT EXISTS `hidden_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))",
            "CREATE TABLE IF NOT EXISTS `renamed_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `customName` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))",
            "CREATE TABLE IF NOT EXISTS `app_categories_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))",
        )

    private val MIGRATION_3_TO_4_FINALIZE =
        listOf(
            "DROP TABLE `hidden_apps`",
            "ALTER TABLE `hidden_apps_new` RENAME TO `hidden_apps`",
            "DROP TABLE `renamed_apps`",
            "ALTER TABLE `renamed_apps_new` RENAME TO `renamed_apps`",
            "DROP TABLE `app_categories`",
            "ALTER TABLE `app_categories_new` RENAME TO `app_categories`",
        )

    private val MIGRATION_4_TO_5_NEW_TABLES =
        listOf(
            "CREATE TABLE IF NOT EXISTS `hidden_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `launcherShortcutId` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`packageName`, `profileKey`, `launcherShortcutId`))",
            "CREATE TABLE IF NOT EXISTS `renamed_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `customName` TEXT NOT NULL, `launcherShortcutId` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`packageName`, `profileKey`, `launcherShortcutId`))",
            "CREATE TABLE IF NOT EXISTS `app_categories_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `category` TEXT NOT NULL, `launcherShortcutId` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`packageName`, `profileKey`, `launcherShortcutId`))",
        )

    private val MIGRATION_4_TO_5_FINALIZE =
        listOf(
            "DROP TABLE `hidden_apps`",
            "ALTER TABLE `hidden_apps_new` RENAME TO `hidden_apps`",
            "DROP TABLE `renamed_apps`",
            "ALTER TABLE `renamed_apps_new` RENAME TO `renamed_apps`",
            "DROP TABLE `app_categories`",
            "ALTER TABLE `app_categories_new` RENAME TO `app_categories`",
        )

    val migration6To7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `indexed_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `treeUri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `lastIndexedAt` INTEGER NOT NULL, `documentCount` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `indexed_documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folderId` INTEGER NOT NULL, `treeUri` TEXT NOT NULL, `documentId` TEXT NOT NULL, `documentUri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_indexed_documents_folderId` ON `indexed_documents` (`folderId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_indexed_documents_displayName` ON `indexed_documents` (`displayName`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_indexed_documents_documentUri` ON `indexed_documents` (`documentUri`)"
                )
            }
        }

    val migration5To6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `suppressed_category_definitions` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))"
                )
            }
        }

    val migration4To5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_4_TO_5_NEW_TABLES.forEach(db::execSQL)

                db.query("SELECT packageName, profileKey FROM hidden_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val profileIndex = cursor.getColumnIndexOrThrow("profileKey")
                    while (cursor.moveToNext()) {
                        db.execSQL(
                                "INSERT OR REPLACE INTO `hidden_apps_new` (`packageName`, `profileKey`, `launcherShortcutId`) VALUES (?, ?, '')",
                                arrayOf(
                                        cursor.getString(packageIndex),
                                        cursor.getString(profileIndex),
                                ),
                        )
                    }
                }

                db.query("SELECT packageName, profileKey, customName FROM renamed_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val profileIndex = cursor.getColumnIndexOrThrow("profileKey")
                    val nameIndex = cursor.getColumnIndexOrThrow("customName")
                    while (cursor.moveToNext()) {
                        db.execSQL(
                                "INSERT OR REPLACE INTO `renamed_apps_new` (`packageName`, `profileKey`, `customName`, `launcherShortcutId`) VALUES (?, ?, ?, '')",
                                arrayOf(
                                        cursor.getString(packageIndex),
                                        cursor.getString(profileIndex),
                                        cursor.getString(nameIndex),
                                ),
                        )
                    }
                }

                db.query("SELECT packageName, profileKey, category FROM app_categories").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val profileIndex = cursor.getColumnIndexOrThrow("profileKey")
                    val categoryIndex = cursor.getColumnIndexOrThrow("category")
                    while (cursor.moveToNext()) {
                        db.execSQL(
                                "INSERT OR REPLACE INTO `app_categories_new` (`packageName`, `profileKey`, `category`, `launcherShortcutId`) VALUES (?, ?, ?, '')",
                                arrayOf(
                                        cursor.getString(packageIndex),
                                        cursor.getString(profileIndex),
                                        cursor.getString(categoryIndex),
                                ),
                        )
                    }
                }

                MIGRATION_4_TO_5_FINALIZE.forEach(db::execSQL)
            }
        }

    fun migration3To4(
        context: Context? = null,
    ) =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_TO_4_NEW_TABLES.forEach(db::execSQL)

                db.query("SELECT packageName FROM hidden_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        db.execSQL(
                            "INSERT OR REPLACE INTO `hidden_apps_new` (`packageName`, `profileKey`) VALUES (?, '0')",
                            arrayOf(packageName),
                        )
                    }
                }

                db.query("SELECT packageName, customName FROM renamed_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val nameIndex = cursor.getColumnIndexOrThrow("customName")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        val customName = cursor.getString(nameIndex)
                        db.execSQL(
                            "INSERT OR REPLACE INTO `renamed_apps_new` (`packageName`, `profileKey`, `customName`) VALUES (?, '0', ?)",
                            arrayOf(packageName, customName),
                        )
                    }
                }

                db.query("SELECT packageName, category FROM app_categories").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val categoryIndex = cursor.getColumnIndexOrThrow("category")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        val category = cursor.getString(categoryIndex)
                        db.execSQL(
                            "INSERT OR REPLACE INTO `app_categories_new` (`packageName`, `profileKey`, `category`) VALUES (?, '0', ?)",
                            arrayOf(packageName, category),
                        )
                    }
                }

                MIGRATION_3_TO_4_FINALIZE.forEach(db::execSQL)
            }
        }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fokus_launcher_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3To4(context), migration4To5, migration5To6, migration6To7)
         .build()
        AppDatabase.instance = db
        return db
    }

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

    @Provides
    @Singleton
    fun provideIndexedDocumentDao(database: AppDatabase): IndexedDocumentDao = database.indexedDocumentDao()

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository = WeatherRepository()

    @Provides
    @Singleton
    fun providePrivateSpaceManager(
        @ApplicationContext context: Context
    ): PrivateSpaceManager = PrivateSpaceManager(context)

    /** CPU-bound drawer work; tests may replace with [Dispatchers.Unconfined]. */
    @Provides
    @Singleton
    @Named("DrawerComputation")
    fun provideDrawerComputationDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** IO dispatcher for disk and network tasks; injectable in tests for determinism. */
    @Provides
    @Singleton
    @Named("IoDispatcher")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
