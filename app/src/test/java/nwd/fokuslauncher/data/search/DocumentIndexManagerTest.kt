package nwd.fokuslauncher.data.search

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nwd.fokuslauncher.data.database.dao.IndexedDocumentDao
import nwd.fokuslauncher.data.database.entity.IndexedDocumentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentIndexManagerTest {

    private lateinit var indexedDocumentDao: IndexedDocumentDao
    private lateinit var documentIndexManager: DocumentIndexManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        indexedDocumentDao = mockk(relaxed = true)
        every { indexedDocumentDao.getAllFoldersFlow() } returns flowOf(emptyList())
        every { indexedDocumentDao.getTotalDocumentCountFlow() } returns flowOf(0)
        documentIndexManager = DocumentIndexManager(indexedDocumentDao, testDispatcher)
    }

    @Test
    fun `searchDocuments returns empty when query is less than 2 chars`() = runTest(testDispatcher) {
        val result = documentIndexManager.searchDocumentsResult("a")
        assertTrue(result is ProviderQueryResult.NoResults)
        assertTrue(documentIndexManager.searchDocuments("a").isEmpty())
    }

    @Test
    fun `searchDocuments returns matched documents`() = runTest(testDispatcher) {
        val entities = listOf(
            IndexedDocumentEntity(
                id = 1L,
                folderId = 10L,
                treeUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
                documentId = "primary:Documents/Annual_Report_2026.pdf",
                documentUri = "content://com.android.providers.downloads.documents/document/1",
                displayName = "Annual_Report_2026.pdf",
                mimeType = "application/pdf",
                sizeBytes = 204800,
                lastModified = 1700000000L
            )
        )
        coEvery { indexedDocumentDao.searchDocuments("Report", 10) } returns entities

        val result = documentIndexManager.searchDocumentsResult("Report", 10)
        assertTrue(result is ProviderQueryResult.Success)
        val data = (result as ProviderQueryResult.Success).data
        assertEquals(1, data.size)
        assertEquals("Annual_Report_2026.pdf", data[0].displayName)
        assertEquals("application/pdf", data[0].mimeType)
    }

    @Test
    fun `searchDocuments returns NoResults when no entities match`() = runTest(testDispatcher) {
        coEvery { indexedDocumentDao.searchDocuments("xyz", 10) } returns emptyList()

        val result = documentIndexManager.searchDocumentsResult("xyz", 10)
        assertTrue(result is ProviderQueryResult.NoResults)
        assertTrue(documentIndexManager.searchDocuments("xyz", 10).isEmpty())
    }

    @Test
    fun `searchDocuments returns ProviderFailure when dao throws exception`() = runTest(testDispatcher) {
        coEvery { indexedDocumentDao.searchDocuments("error", 10) } throws RuntimeException("SQLite read error")

        val result = documentIndexManager.searchDocumentsResult("error", 10)
        assertTrue(result is ProviderQueryResult.ProviderFailure)
        val failure = result as ProviderQueryResult.ProviderFailure
        assertEquals("RuntimeException", failure.exceptionType)
        assertEquals("SQLite read error", failure.message)
        assertTrue(documentIndexManager.searchDocuments("error", 10).isEmpty())
    }
}
