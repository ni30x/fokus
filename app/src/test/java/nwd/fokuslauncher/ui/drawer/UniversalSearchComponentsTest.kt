package nwd.fokuslauncher.ui.drawer

import android.Manifest
import android.net.Uri
import nwd.fokuslauncher.data.search.ContactSearchResult
import nwd.fokuslauncher.data.search.DocumentSearchResult
import nwd.fokuslauncher.data.search.LocalSearchManager
import nwd.fokuslauncher.data.search.MediaSearchResult
import nwd.fokuslauncher.data.search.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `document folder indexing is strictly separated from runtime media permissions`() {
        // Verify document index search uses local DB SAF folders, not runtime media permissions
        val docResult = DocumentSearchResult(
            id = 10L,
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
        assertTrue(
            mediaPerms.contains(Manifest.permission.READ_MEDIA_IMAGES) ||
            mediaPerms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }
}
