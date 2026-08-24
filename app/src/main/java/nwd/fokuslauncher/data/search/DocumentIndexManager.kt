package nwd.fokuslauncher.data.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import nwd.fokuslauncher.data.database.dao.IndexedDocumentDao
import nwd.fokuslauncher.data.database.entity.IndexedDocumentEntity
import nwd.fokuslauncher.data.database.entity.IndexedFolderEntity
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

data class DocumentSearchResult(
    val id: Long,
    val folderId: Long,
    val displayName: String,
    val uri: Uri,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long
)

@Singleton
class DocumentIndexManager @Inject constructor(
    private val indexedDocumentDao: IndexedDocumentDao,
    @Named("IoDispatcher") private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: Flow<Boolean> = _isIndexing.asStateFlow()

    val indexedFoldersFlow: Flow<List<IndexedFolderEntity>> = indexedDocumentDao.getAllFoldersFlow()
    val totalDocumentCountFlow: Flow<Int> = indexedDocumentDao.getTotalDocumentCountFlow()

    suspend fun searchDocumentsResult(query: String, limit: Int = 10): ProviderQueryResult<DocumentSearchResult> =
        withContext(ioDispatcher) {
            val trimmed = query.trim()
            if (trimmed.length < 2) return@withContext ProviderQueryResult.NoResults

            try {
                val entities = indexedDocumentDao.searchDocuments(trimmed, limit)
                if (entities.isEmpty()) {
                    return@withContext ProviderQueryResult.NoResults
                }
                val results = entities.map { entity ->
                    DocumentSearchResult(
                        id = entity.id,
                        folderId = entity.folderId,
                        displayName = entity.displayName,
                        uri = Uri.parse(entity.documentUri),
                        mimeType = entity.mimeType,
                        sizeBytes = entity.sizeBytes,
                        lastModified = entity.lastModified
                    )
                }
                ProviderQueryResult.Success(results)
            } catch (e: Exception) {
                if (Log.isLoggable("LocalSearch", Log.DEBUG)) {
                    Log.d(
                        "LocalSearch",
                        "Provider: ${ProviderType.DOCUMENTS} | Stage: ${QueryStage.RESOLVER_QUERY} | URI: none | Exception: ${e.javaClass.simpleName}(${e.message})"
                    )
                }
                ProviderQueryResult.ProviderFailure(e.javaClass.simpleName, e.message)
            }
        }

    suspend fun searchDocuments(query: String, limit: Int = 10): List<DocumentSearchResult> =
        searchDocumentsResult(query, limit).getOrEmpty()

    suspend fun addFolder(treeUri: Uri, context: Context): Result<IndexedFolderEntity> =
        withContext(ioDispatcher) {
            try {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (e: Exception) {
                    Log.w("DocumentIndexManager", "Failed to take persistable URI permission", e)
                }

                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val folderName = getFolderDisplayName(context, treeUri, docId)

                val existing = indexedDocumentDao.getFolderByUri(treeUri.toString())
                val folderId = if (existing != null) {
                    existing.id
                } else {
                    val newFolder = IndexedFolderEntity(
                        treeUri = treeUri.toString(),
                        displayName = folderName,
                        addedAt = System.currentTimeMillis(),
                        lastIndexedAt = System.currentTimeMillis(),
                        documentCount = 0
                    )
                    indexedDocumentDao.insertFolder(newFolder)
                }

                val folder = indexedDocumentDao.getFolderById(folderId) ?: IndexedFolderEntity(
                    id = folderId,
                    treeUri = treeUri.toString(),
                    displayName = folderName
                )

                // Trigger indexing for this folder
                reindexFolderInternal(context, folder)

                Result.success(folder)
            } catch (e: Exception) {
                Log.e("DocumentIndexManager", "Error adding folder $treeUri", e)
                Result.failure(e)
            }
        }

    suspend fun removeFolder(folderId: Long, treeUriString: String?, context: Context) =
        withContext(ioDispatcher) {
            try {
                treeUriString?.let { uriStr ->
                    try {
                        val uri = Uri.parse(uriStr)
                        context.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        Log.w("DocumentIndexManager", "Error releasing URI permission", e)
                    }
                }
                indexedDocumentDao.deleteFolderAndDocuments(folderId)
            } catch (e: Exception) {
                Log.e("DocumentIndexManager", "Error removing folder $folderId", e)
            }
        }

    suspend fun reindexAllFolders(context: Context) = withContext(ioDispatcher) {
        _isIndexing.value = true
        try {
            val folders = indexedDocumentDao.getAllFolders()
            for (folder in folders) {
                reindexFolderInternal(context, folder)
            }
        } finally {
            _isIndexing.value = false
        }
    }

    private suspend fun reindexFolderInternal(context: Context, folder: IndexedFolderEntity) {
        try {
            val treeUri = Uri.parse(folder.treeUri)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val scannedDocuments = mutableListOf<IndexedDocumentEntity>()

            scanFolderRecursive(
                context = context,
                treeUri = treeUri,
                folderId = folder.id,
                currentDocId = docId,
                results = scannedDocuments,
                maxDepth = 6,
                currentDepth = 0
            )

            val updatedFolder = folder.copy(
                lastIndexedAt = System.currentTimeMillis(),
                documentCount = scannedDocuments.size
            )

            indexedDocumentDao.replaceDocumentsForFolder(
                folderId = folder.id,
                documents = scannedDocuments,
                updatedFolder = updatedFolder
            )
        } catch (e: Exception) {
            Log.e("DocumentIndexManager", "Failed to index folder ${folder.displayName}", e)
        }
    }

    private fun getFolderDisplayName(context: Context, treeUri: Uri, docId: String): String {
        try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (e: Exception) {
            Log.w("DocumentIndexManager", "Could not query folder display name", e)
        }
        val lastSegment = treeUri.lastPathSegment ?: "Folder"
        return lastSegment.substringAfterLast(':').ifBlank { "Folder" }
    }

    private fun scanFolderRecursive(
        context: Context,
        treeUri: Uri,
        folderId: Long,
        currentDocId: String,
        results: MutableList<IndexedDocumentEntity>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 5000) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "Document"
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                    val lastMod = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else System.currentTimeMillis()

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanFolderRecursive(
                            context = context,
                            treeUri = treeUri,
                            folderId = folderId,
                            currentDocId = childDocId,
                            results = results,
                            maxDepth = maxDepth,
                            currentDepth = currentDepth + 1
                        )
                    } else {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        results.add(
                            IndexedDocumentEntity(
                                folderId = folderId,
                                treeUri = treeUri.toString(),
                                documentId = childDocId,
                                documentUri = docUri.toString(),
                                displayName = name,
                                mimeType = mime,
                                sizeBytes = size,
                                lastModified = lastMod
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("DocumentIndexManager", "Error reading children for docId $currentDocId: ${e.message}")
        }
    }
}
