package nwd.fokuslauncher.data.search

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
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

@RunWith(RobolectricTestRunner::class)
class LocalSearchManagerTest {

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
    fun `short query returns NoResults`() {
        val result = LocalSearchManager.searchContactsResult(context, "a")
        assertTrue(result is ProviderQueryResult.NoResults)
        assertEquals(emptyList<ContactSearchResult>(), result.getOrEmpty())
    }

    @Test
    fun `missing contacts permission returns PermissionRequired`() {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) } returns PackageManager.PERMISSION_DENIED
        val result = LocalSearchManager.searchContactsResult(context, "John")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
        assertEquals(emptyList<ContactSearchResult>(), result.getOrEmpty())
    }

    @Test
    fun `searchCallLogs returns PermissionRequired when permission denied`() {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) } returns PackageManager.PERMISSION_DENIED
        val result = LocalSearchManager.searchCallLogsResult(context, "12345")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `searchMessages returns PermissionRequired when permission denied`() {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) } returns PackageManager.PERMISSION_DENIED
        val result = LocalSearchManager.searchMessagesResult(context, "hello")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `searchCalendar returns PermissionRequired when permission denied`() {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) } returns PackageManager.PERMISSION_DENIED
        val result = LocalSearchManager.searchCalendarResult(context, "meeting")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `searchMediaResult returns PermissionRequired when all media permissions denied`() = runTest {
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_DENIED
        val result = LocalSearchManager.searchMediaResult(context, "vacation")
        assertTrue(result is ProviderQueryResult.PermissionRequired)
    }

    @Test
    fun `searchMediaCollection image filename match returns media results`() = runTest {
        val matrixCursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        ))
        matrixCursor.addRow(arrayOf<Any?>(101L, "vacation_beach.jpg", 1024L, "image/jpeg", "Beach Vacation"))

        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns matrixCursor

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "vacation",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success<*>)
        val list = (result as ProviderQueryResult.Success<MediaSearchResult>).data
        assertEquals(1, list.size)
        assertEquals("vacation_beach.jpg", list[0].displayName)
        assertEquals(MediaType.IMAGE, list[0].mediaType)
        assertEquals(1024L, list[0].sizeBytes)
    }

    @Test
    fun `searchMediaCollection video filename match returns video results`() = runTest {
        val matrixCursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        ))
        matrixCursor.addRow(arrayOf<Any?>(202L, "birthday_party.mp4", 5048000L, "video/mp4", "Party Video"))

        every {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns matrixCursor

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO,
            query = "birthday",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success<*>)
        val list = (result as ProviderQueryResult.Success<MediaSearchResult>).data
        assertEquals(1, list.size)
        assertEquals("birthday_party.mp4", list[0].displayName)
        assertEquals(MediaType.VIDEO, list[0].mediaType)
    }

    @Test
    fun `searchMediaCollection audio filename match returns audio results`() = runTest {
        val matrixCursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        ))
        matrixCursor.addRow(arrayOf<Any?>(303L, "podcast_episode_1.mp3", 2048000L, "audio/mpeg", "Podcast Ep 1"))

        every {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns matrixCursor

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.AUDIO,
            query = "podcast",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.Success<*>)
        val list = (result as ProviderQueryResult.Success<MediaSearchResult>).data
        assertEquals(1, list.size)
        assertEquals("podcast_episode_1.mp3", list[0].displayName)
        assertEquals(MediaType.AUDIO, list[0].mediaType)
    }

    @Test
    fun `searchContacts match returns contact results with phone number`() {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) } returns PackageManager.PERMISSION_GRANTED

        val contactsCursor = MatrixCursor(arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        ))
        contactsCursor.addRow(arrayOf<Any?>("10", "Alice Smith", 1, null))

        val phoneCursor = MatrixCursor(arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ))
        phoneCursor.addRow(arrayOf<Any?>("10", "+1234567890"))

        every {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns contactsCursor

        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns phoneCursor

        val result = LocalSearchManager.searchContactsResult(context, "Alice")
        assertTrue(result is ProviderQueryResult.Success<*>)
        val list = (result as ProviderQueryResult.Success<ContactSearchResult>).data
        assertEquals(1, list.size)
        assertEquals("Alice Smith", list[0].displayName)
        assertEquals("+1234567890", list[0].phoneNumber)
    }

    @Test
    fun `empty cursor returns NoResults`() {
        val emptyCursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        ))

        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyCursor

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "nonexistent",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.NoResults)
        assertTrue(result.getOrEmpty().isEmpty())
    }

    @Test
    fun `provider query exception returns ProviderFailure`() {
        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } throws IllegalStateException("Database disk image is malformed")

        val result = LocalSearchManager.queryMediaCollection(
            context = context,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            query = "photo",
            limit = 10
        )

        assertTrue(result is ProviderQueryResult.ProviderFailure)
        val failure = result as ProviderQueryResult.ProviderFailure
        assertEquals("IllegalStateException", failure.exceptionType)
        assertTrue(failure.message?.contains("malformed") == true)
    }

    @Test
    fun `partial media access only queries granted media type`() = runTest {
        // Mock only image permission granted (or visual media), audio denied
        every { ContextCompat.checkSelfPermission(context, any()) } answers {
            val perm = invocation.args[1] as String
            if (perm.contains("IMAGE") || perm == Manifest.permission.READ_EXTERNAL_STORAGE) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        }

        val imageCursor = MatrixCursor(arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.TITLE
        ))
        imageCursor.addRow(arrayOf<Any?>(55L, "image_test.png", 500L, "image/png", "Test Image"))

        every {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns imageCursor

        val result = LocalSearchManager.searchMediaResult(context, "test")
        assertTrue(result is ProviderQueryResult.Success<*>)
        val items = (result as ProviderQueryResult.Success<MediaSearchResult>).data
        assertEquals(1, items.size)
        assertEquals("image_test.png", items[0].displayName)
    }
}

