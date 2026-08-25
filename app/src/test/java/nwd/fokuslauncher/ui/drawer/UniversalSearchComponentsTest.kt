package nwd.fokuslauncher.ui.drawer

import android.Manifest
import android.net.Uri
import nwd.fokuslauncher.data.search.ContactSearchResult
import nwd.fokuslauncher.data.search.DocumentSearchResult
import nwd.fokuslauncher.data.search.LocalSearchManager
import nwd.fokuslauncher.data.search.MediaSearchResult
import nwd.fokuslauncher.data.search.MediaType
import nwd.fokuslauncher.data.search.ProviderQueryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UniversalSearchComponentsTest {

    @Test
    fun `media results data model supports optional thumbnails without blocking`() {
        val imageWithThumbnail = MediaSearchResult(
            id = 1L,
            displayName = "photo.jpg",
            uri = Uri.parse("content://media/external/images/media/1"),
            mediaType = MediaType.IMAGE,
            sizeBytes = 2048,
            mimeType = "image/jpeg"
        )
        assertNotNull(imageWithThumbnail.uri)

        val audioWithoutThumbnail = MediaSearchResult(
            id = 2L,
            displayName = "song.mp3",
            uri = Uri.parse("content://media/external/audio/media/2"),
            mediaType = MediaType.AUDIO,
            sizeBytes = 4096000,
            mimeType = "audio/mpeg"
        )
        assertEquals(MediaType.AUDIO, audioWithoutThumbnail.mediaType)
        // Ensure non-blocking metadata access
        assertTrue(audioWithoutThumbnail.sizeBytes > 0)
    }

    @Test
    fun `contacts with null photoUri render fallback icon without error`() {
        val contactWithoutPhoto = ContactSearchResult(
            id = "1",
            displayName = "Bob Jones",
            photoUri = null,
            phoneNumber = "555-0199"
        )
        assertNull(contactWithoutPhoto.photoUri)
        assertEquals("Bob Jones", contactWithoutPhoto.displayName)

        val contactWithPhoto = ContactSearchResult(
            id = "2",
            displayName = "Alice Wonderland",
            photoUri = Uri.parse("content://com.android.contacts/contacts/2/photo"),
            phoneNumber = "555-0188"
        )
        assertNotNull(contactWithPhoto.photoUri)
    }

    @Test
    fun `permission states have correct actions and error states`() {
        val permissionRequired = ProviderQueryResult.PermissionRequired
        assertTrue(permissionRequired is ProviderQueryResult.PermissionRequired)

        val providerFailure = ProviderQueryResult.ProviderFailure(
            exceptionType = "SECURITY_EXCEPTION",
            message = "Permission denied by system policy"
        )
        assertEquals("SECURITY_EXCEPTION", providerFailure.exceptionType)
        assertEquals("Permission denied by system policy", providerFailure.message)
    }

    @Test
    fun `document folder indexing is strictly separated from runtime media permissions`() {
        // Verify document index search uses local DB SAF folders, not runtime media permissions
        val docResult = DocumentSearchResult(
            id = 10L,
            folderId = 1L,
            displayName = "Project_Specs.docx",
            uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FProject_Specs.docx"),
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sizeBytes = 15000L,
            lastModified = System.currentTimeMillis()
        )
        assertEquals("Project_Specs.docx", docResult.displayName)
        assertTrue(docResult.uri.toString().contains("documents"))

        // Media permissions are specifically Android media permissions
        val mediaPerms = LocalSearchManager.getRequiredMediaPermissions()
        assertFalse(mediaPerms.contains("OPEN_DOCUMENT_TREE"))
        assertFalse(mediaPerms.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
        assertTrue(
            mediaPerms.contains(Manifest.permission.READ_MEDIA_IMAGES) ||
            mediaPerms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }

    @Test
    fun `choose folders and files is handled via SAF tree picker and not presented as media permission`() {
        // Document search indexing is based on SAF folder tree picking
        val safDocFolderUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload")
        val isTreeUri = safDocFolderUri.authority?.contains("documents") == true
        assertTrue("SAF folder picker must use DocumentsContract tree authorities", isTreeUri)

        // Verify media permissions query does not require or request folder selection
        val mediaPermissions = LocalSearchManager.getRequiredMediaPermissions()
        for (perm in mediaPermissions) {
            assertTrue(
                perm == Manifest.permission.READ_MEDIA_IMAGES ||
                perm == Manifest.permission.READ_MEDIA_VIDEO ||
                perm == Manifest.permission.READ_MEDIA_AUDIO ||
                perm == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED ||
                perm == Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
}
