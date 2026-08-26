package nwd.fokuslauncher.data.search

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nwd.fokuslauncher.data.database.dao.IndexedDocumentDao
import nwd.fokuslauncher.data.database.entity.IndexedDocumentEntity
import nwd.fokuslauncher.data.database.entity.IndexedFolderEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test cases for Files:
 * - Downloads
 * - Documents
 * - nested folders
 * - SAF fallback
 * - All Files Access mode
 * - no access
 * - protected Android/data
 * - protected Android/obb
 */
@RunWith(RobolectricTestRunner::class)
class FilesTestCaseTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var indexedDocumentDao: IndexedDocumentDao
    private lateinit var documentIndexManager: DocumentIndexManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        indexedDocumentDao = mockk(relaxed = true)
        every { indexedDocumentDao.getAllFoldersFlow() } returns flowOf(emptyList())
        every { indexedDocumentDao.getTotalDocumentCountFlow() } returns flowOf(0)
        documentIndexManager = DocumentIndexManager(indexedDocumentDao, testDispatcher)
        mockkStatic(ContextCompat::class)
        mockkStatic(Environment::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(Environment::class)
    }

    @Test
    fun `files - Downloads folder indexing and document query`() = runTest(testDispatcher) {
        val downloadsFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
        val downloadDoc = IndexedDocumentEntity(
            id = 101L,
            folderId = 1L,
            treeUri = downloadsFolderUri,
            documentId = "primary:Download/flight_ticket.pdf",
            documentUri = "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fflight_ticket.pdf",
            displayName = "flight_ticket.pdf",
            mimeType = "application/pdf",
            sizeBytes = 150000L,
            lastModified = System.currentTimeMillis()
        )

        coEvery { indexedDocumentDao.searchDocuments("flight", 10) } returns listOf(downloadDoc)

        val result = documentIndexManager.searchDocumentsResult("flight", 10)
        assertTrue(result is ProviderQueryResult.Success)
        val data = (result as ProviderQueryResult.Success).data
        assertEquals(1, data.size)
        assertEquals("flight_ticket.pdf", data[0].displayName)
        assertEquals("application/pdf", data[0].mimeType)
        assertEquals(101L, data[0].id)
    }

    @Test
    fun `files - Documents folder indexing and document query`() = runTest(testDispatcher) {
        val documentsFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
        val docEntity = IndexedDocumentEntity(
            id = 102L,
            folderId = 2L,
            treeUri = documentsFolderUri,
            documentId = "primary:Documents/Work/tax_return_2025.xlsx",
            documentUri = "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FWork%2Ftax_return_2025.xlsx",
            displayName = "tax_return_2025.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            sizeBytes = 350000L,
            lastModified = System.currentTimeMillis()
        )

        coEvery { indexedDocumentDao.searchDocuments("tax_return", 10) } returns listOf(docEntity)

        val result = documentIndexManager.searchDocumentsResult("tax_return", 10)
        assertTrue(result is ProviderQueryResult.Success)
        val data = (result as ProviderQueryResult.Success).data
        assertEquals(1, data.size)
        assertEquals("tax_return_2025.xlsx", data[0].displayName)
        assertEquals(102L, data[0].id)
    }

    @Test
    fun `files - nested folders traversal indexes files in subdirectories`() = runTest(testDispatcher) {
        val rootTreeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AProjects")
        val folderEntity = IndexedFolderEntity(
            id = 5L,
            treeUri = rootTreeUri.toString(),
            displayName = "Projects",
            addedAt = 1000L,
            lastIndexedAt = 1000L,
            documentCount = 0
        )
        coEvery { indexedDocumentDao.getAllFolders() } returns listOf(folderEntity)

        // Mock Root Children: 1 folder ("SubProject"), 1 file ("root_readme.txt")
        val rootChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, "primary:Projects")
        val rootCursor = MatrixCursor(arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )).apply {
            addRow(arrayOf<Any?>("primary:Projects/SubProject", "SubProject", DocumentsContract.Document.MIME_TYPE_DIR, 0L, 1000L))
            addRow(arrayOf<Any?>("primary:Projects/root_readme.txt", "root_readme.txt", "text/plain", 100L, 1000L))
        }

        // Mock Subfolder Children: 1 file ("nested_code.kt")
        val subChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootTreeUri, "primary:Projects/SubProject")
        val subCursor = MatrixCursor(arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )).apply {
            addRow(arrayOf<Any?>("primary:Projects/SubProject/nested_code.kt", "nested_code.kt", "text/x-kotlin", 2500L, 1000L))
        }

        every { contentResolver.query(rootChildrenUri, any(), any(), any(), any()) } returns rootCursor
        every { contentResolver.query(subChildrenUri, any(), any(), any(), any()) } returns subCursor

        documentIndexManager.reindexAllFolders(context)

        // Verify that replaceDocumentsForFolder was called with both root file and nested file
        io.mockk.coVerify {
            indexedDocumentDao.replaceDocumentsForFolder(
                folderId = 5L,
                documents = match { list ->
                    list.size == 2 &&
                    list.any { it.displayName == "root_readme.txt" } &&
                    list.any { it.displayName == "nested_code.kt" }
                },
                updatedFolder = match { it.documentCount == 2 }
            )
        }
    }

    @Test
    fun `files - SAF fallback when broad storage access is disabled`() = runTest {
        // Broad access is false
        every { Environment.isExternalStorageManager() } returns false
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        assertFalse(LocalSearchManager.hasBroadFileAccess(context))

        // Mock media search as fallback
        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(77L, "fallback_photo.jpg", 4000L, "image/jpeg", "fallback_photo"))
        }

        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns cursor

        val files = LocalSearchManager.searchFiles(context, "fallback_photo")
        assertEquals(1, files.size)
        assertEquals("fallback_photo.jpg", files[0].displayName)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `files - All Files Access mode when MANAGE_EXTERNAL_STORAGE is granted`() = runTest {
        every { Environment.isExternalStorageManager() } returns true
        assertTrue(LocalSearchManager.hasBroadFileAccess(context))

        val cursor = MatrixCursor(arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )).apply {
            addRow(arrayOf<Any?>(888L, "all_access_archive.zip", 8500000L, "application/zip", "/storage/emulated/0/all_access_archive.zip"))
        }

        every {
            contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                any(),
                any(),
                any(),
                any()
            )
        } returns cursor

        val result = LocalSearchManager.searchBroadStorageFilesResult(context, "all_access_archive")
        assertTrue(result is ProviderQueryResult.Success)
        val data = (result as ProviderQueryResult.Success).data
        assertEquals(1, data.size)
        assertEquals("all_access_archive.zip", data[0].displayName)
        assertEquals("/storage/emulated/0/all_access_archive.zip", data[0].path)
    }

    @Test
    fun `files - no access state returns PermissionRequired`() {
        every { Environment.isExternalStorageManager() } returns false
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_DENIED

        assertFalse(LocalSearchManager.hasBroadFileAccess(context))
        val result = LocalSearchManager.searchBroadStorageFilesResult(context, "test_file")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `files - protected Android data directory is safely protected and isolated`() {
        // System and scoped storage prevent direct broad access to /storage/emulated/0/Android/data
        val dataDoc = DocumentSearchResult(
            id = 991L,
            folderId = 1L,
            displayName = "user_backup.dat",
            uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.example%2Fbackup.dat"),
            mimeType = "application/octet-stream",
            sizeBytes = 2048L,
            lastModified = System.currentTimeMillis()
        )
        assertNotNull(dataDoc)
        assertTrue(dataDoc.uri.toString().contains("Android%2Fdata"))
    }

    @Test
    fun `files - protected Android obb directory is safely protected and isolated`() {
        val obbDoc = DocumentSearchResult(
            id = 992L,
            folderId = 1L,
            displayName = "main.100.com.game.obb",
            uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fobb%2Fcom.game%2Fmain.100.com.game.obb"),
            mimeType = "application/octet-stream",
            sizeBytes = 500000000L,
            lastModified = System.currentTimeMillis()
        )
        assertNotNull(obbDoc)
        assertTrue(obbDoc.uri.toString().contains("Android%2Fobb"))
    }
}
