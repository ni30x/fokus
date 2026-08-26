package nwd.fokuslauncher.data.search

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test cases for Thumbnails:
 * - JPEG exact filename
 * - PNG exact filename
 * - MP4 exact filename
 * - image under partial permission
 * - image under full permission
 * - video thumbnail
 * - media with revoked access
 * - uncached media
 * - cached media
 */
@RunWith(RobolectricTestRunner::class)
class ThumbnailsTestCaseTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `thumbnails - JPEG exact filename match returns correct image media result`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(101L, "vacation_sunset.jpg", 1024000L, "image/jpeg", "vacation_sunset"))
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

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "vacation_sunset.jpg",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success)
        val items = (result as ProviderQueryResult.Success).data
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("vacation_sunset.jpg", item.displayName)
        assertEquals("image/jpeg", item.mimeType)
        assertEquals(MediaType.IMAGE, item.mediaType)
        assertEquals(Uri.parse("content://media/external/images/media/101"), item.uri)
    }

    @Test
    fun `thumbnails - PNG exact filename match returns correct image media result`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(102L, "app_mockup_v2.png", 512000L, "image/png", "app_mockup_v2"))
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

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "app_mockup_v2.png",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success)
        val items = (result as ProviderQueryResult.Success).data
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("app_mockup_v2.png", item.displayName)
        assertEquals("image/png", item.mimeType)
        assertEquals(MediaType.IMAGE, item.mediaType)
        assertEquals(Uri.parse("content://media/external/images/media/102"), item.uri)
    }

    @Test
    fun `thumbnails - MP4 exact filename match returns correct video media result`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(201L, "screen_recording_01.mp4", 15400000L, "video/mp4", "screen_recording_01"))
        }

        every {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns cursor

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO,
            query = "screen_recording_01.mp4",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success)
        val items = (result as ProviderQueryResult.Success).data
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("screen_recording_01.mp4", item.displayName)
        assertEquals("video/mp4", item.mimeType)
        assertEquals(MediaType.VIDEO, item.mediaType)
        assertEquals(Uri.parse("content://media/external/video/media/201"), item.uri)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `thumbnails - image under partial permission identifies partial state and executes visual queries`() = runTest {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) } returns PackageManager.PERMISSION_GRANTED

        val state = LocalSearchManager.getMediaPermissionState(context)
        assertEquals(MediaPermissionState.PARTIAL, state)
        assertTrue(LocalSearchManager.hasVisualMediaPermission(context))
        assertTrue(LocalSearchManager.hasImagesPermission(context))
        assertTrue(LocalSearchManager.hasVideoPermission(context))

        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(301L, "user_selected_photo.jpg", 800000L, "image/jpeg", "user_selected_photo"))
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

        val result = LocalSearchManager.searchMediaResult(context, "user_selected_photo")
        assertTrue(result is ProviderQueryResult.Success)
        val items = (result as ProviderQueryResult.Success).data
        assertEquals(1, items.size)
        assertEquals("user_selected_photo.jpg", items[0].displayName)
    }

    @Test
    fun `thumbnails - image under full permission returns full access state`() {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val state = LocalSearchManager.getMediaPermissionState(context)
        assertEquals(MediaPermissionState.FULL, state)
        assertTrue(LocalSearchManager.hasVisualMediaPermission(context))
        assertTrue(LocalSearchManager.hasImagesPermission(context))
    }

    @Test
    fun `thumbnails - video thumbnail metadata distinguishes video type correctly`() {
        val videoResult = MediaSearchResult(
            id = 401L,
            displayName = "drone_footage.mp4",
            uri = Uri.parse("content://media/external/video/media/401"),
            mimeType = "video/mp4",
            sizeBytes = 25000000L,
            mediaType = MediaType.VIDEO
        )

        assertEquals(MediaType.VIDEO, videoResult.mediaType)
        assertEquals("video/mp4", videoResult.mimeType)
        assertNotNull(videoResult.uri)
    }

    @Test
    fun `thumbnails - media with revoked access returns PermissionRequired gracefully`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED
        every {
            contentResolver.query(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws SecurityException("Permission revoked by user at runtime")

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "private_pic",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `thumbnails - uncached media query creates fresh valid MediaSearchResult list`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        )).apply {
            addRow(arrayOf<Any?>(501L, "fresh_image_1.jpg", 1000L, "image/jpeg", "fresh_image_1"))
            addRow(arrayOf<Any?>(502L, "fresh_image_2.jpg", 2000L, "image/jpeg", "fresh_image_2"))
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

        val result = LocalSearchManager.searchMediaResult(context, "fresh_image")
        assertTrue(result is ProviderQueryResult.Success)
        val data = (result as ProviderQueryResult.Success).data
        assertEquals(2, data.size)
        assertEquals("fresh_image_1.jpg", data[0].displayName)
        assertEquals("fresh_image_2.jpg", data[1].displayName)
    }

    @Test
    fun `thumbnails - cached media deduplication preserves original results accurately`() {
        val doc1 = DocumentSearchResult(
            id = 1L,
            folderId = 10L,
            displayName = "photo_cache.jpg",
            uri = Uri.parse("content://media/external/images/media/1"),
            mimeType = "image/jpeg",
            sizeBytes = 5000L,
            lastModified = 1000L
        )
        val broadFileDuplicate = FileSearchResult(
            id = 1L,
            displayName = "photo_cache.jpg",
            path = "/storage/emulated/0/DCIM/photo_cache.jpg",
            sizeBytes = 5000L,
            uri = Uri.parse("content://media/external/images/media/1"),
            mimeType = "image/jpeg"
        )
        val broadFileNew = FileSearchResult(
            id = 2L,
            displayName = "unique_file.pdf",
            path = "/storage/emulated/0/Download/unique_file.pdf",
            sizeBytes = 12000L,
            uri = Uri.parse("content://media/external/file/2"),
            mimeType = "application/pdf"
        )

        val deduplicated = LocalSearchManager.deduplicateDocumentsAndFiles(
            safDocs = listOf(doc1),
            broadFiles = listOf(broadFileDuplicate, broadFileNew)
        )

        assertEquals(2, deduplicated.size)
        assertEquals("photo_cache.jpg", deduplicated[0].displayName)
        assertEquals("unique_file.pdf", deduplicated[1].displayName)
    }
}
