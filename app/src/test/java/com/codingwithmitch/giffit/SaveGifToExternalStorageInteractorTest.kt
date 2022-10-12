package com.codingwithmitch.giffit

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.domain.RealVersionProvider
import com.codingwithmitch.giffit.interactors.BuildGifInteractor.Companion.saveGifToInternalStorage
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorageInteractor
import com.codingwithmitch.giffit.util.buildBitmapByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveGifToExternalStorageInteractorTest {

    private lateinit var saveGifToExternalStorageInteractor: SaveGifToExternalStorageInteractor
    private val versionProvider = RealVersionProvider()

    @Test
    fun `save to internal storage on API 29+`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
            mock {
                // Set API = 29
                on { provideVersion() } doReturn 29
            }
        )

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val cachedUri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = versionProvider
        )

        // Create mock checkFilePermissions so we can verify it's never called
        val checkFilePermissions: () -> Boolean = mock()

        // Create mock launchPermissionRequest so we can verify it's never called
        val launchPermissionRequest: () -> Unit = mock()

        // Configure ContentResolver. This is required in unit tests with Roboelectric.
        // Otherwise the inputstream will not write anything.
        val shadowContentResolver = Shadows.shadowOf(contentResolver)
        shadowContentResolver.registerInputStream(cachedUri, ByteArrayInputStream(ByteArray(byteArray.size)))

        // Save to external storage
        val emissions = saveGifToExternalStorageInteractor.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = cachedUri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Data<Unit>(Unit)))
        assertThat(emissions[2], equalTo(DataState.Loading<Uri>(DataState.Loading.LoadingState.Idle)))

        // API 29+ these should never be called.
        verify(launchPermissionRequest, never()).invoke()
        verify(checkFilePermissions, never()).invoke()
    }

    @Test
    fun `save to internal storage on API 28- and file permission accepted`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
            mock {
                // Set API = 28
                on { provideVersion() } doReturn 28
            }
        )

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val cachedUri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = RealVersionProvider()
        )

        // User has accepted read/write permission to external storage
        val checkFilePermissions: () -> Boolean = mock {
            on { invoke() } doReturn true
        }

        // Will not be called since checkFilePermissions returns true
        val launchPermissionRequest: () -> Unit = mock()

        // Configure ContentResolver. This is required in unit tests with Roboelectric.
        // Otherwise the inputstream will not write anything.
        val shadowContentResolver = Shadows.shadowOf(contentResolver)
        shadowContentResolver.registerInputStream(cachedUri, ByteArrayInputStream(ByteArray(byteArray.size)))

        // Save to external storage
        val emissions = saveGifToExternalStorageInteractor.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = cachedUri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()


        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Data<Unit>(Unit)))
        assertThat(emissions[2], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Idle)))

        // checkFilePermissions should be called once.
        verify(checkFilePermissions, times(1)).invoke()

        // launchPermissionRequest should not be called since checkFilePermissions returns true
        verify(launchPermissionRequest, never()).invoke()
    }

    @Test
    fun `invoke launchPermissionRequest if file permission false`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
            mock {
                // Set API = 28
                on { provideVersion() } doReturn 28
            }
        )

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val cachedUri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = RealVersionProvider()
        )

        // User has NOT accepted read/write permission to external storage
        val checkFilePermissions: () -> Boolean = mock {
            on { invoke() } doReturn false
        }

        // Will be invoked since checkFilePermissions returns false
        val launchPermissionRequest: () -> Unit = mock()

        // Save to external storage
        // We don't need to configure the ShadowContentResolver since file permission has not
        // been accepted.
        val emissions = saveGifToExternalStorageInteractor.execute(
            contentResolver = contentResolver,
            context = context,
            cachedUri = cachedUri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Idle)))

        // checkFilePermissions should be called once.
        verify(checkFilePermissions, times(1)).invoke()

        // launchPermissionRequest should be called once.
        verify(launchPermissionRequest, times(1)).invoke()
    }

    @Test
    fun `API 29+ throws exception with bad content resolver`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
            mock {
                // Set API = 29
                on { provideVersion() } doReturn 29
            }
        )

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val cachedUri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = RealVersionProvider()
        )

        // Create mock checkFilePermissions so we can verify it's never called
        val checkFilePermissions: () -> Boolean = mock()

        // Create mock launchPermissionRequest so we can verify it's never called
        val launchPermissionRequest: () -> Unit = mock()

        // Force an exception when trying to insert the Uri
        val badContentResolver: ContentResolver = mock {
            on { insert(any(), any()) } doThrow IllegalArgumentException("Something is busted")
        }

        // Save to external storage
        val emissions = saveGifToExternalStorageInteractor.execute(
            contentResolver = badContentResolver,
            context = context,
            cachedUri = cachedUri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Unit>("Something is busted")))
        assertThat(emissions[2], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Idle)))

        // API 29+ these should never be called.
        verify(launchPermissionRequest, never()).invoke()
        verify(checkFilePermissions, never()).invoke()
    }

    @Test
    fun `API 28- throws exception with bad content resolver`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        saveGifToExternalStorageInteractor = SaveGifToExternalStorageInteractor(
            mock {
                // Set API = 29
                on { provideVersion() } doReturn 28
            }
        )

        // Create a Bitmap
        val byteArray = buildBitmapByteArray(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val cachedUri = saveGifToInternalStorage(
            contentResolver = contentResolver,
            bytes = byteArray,
            cacheProvider = cacheProvider,
            versionProvider = versionProvider
        )

        // Create mock checkFilePermissions so we can verify it's never called
        val checkFilePermissions: () -> Boolean = mock {
            on { invoke() } doReturn true
        }

        // Create mock launchPermissionRequest so we can verify it's never called
        val launchPermissionRequest: () -> Unit = mock()

        // Force an exception when trying to open an InputStream.
        val badContentResolver: ContentResolver = mock {
            on { openInputStream(any()) } doThrow IllegalArgumentException("Something is busted")
        }

        // Save to external storage
        val emissions = saveGifToExternalStorageInteractor.execute(
            contentResolver = badContentResolver,
            context = context,
            cachedUri = cachedUri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()

        // Confirm the emissions are correct.
        assertThat(emissions[0], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Active())))
        assertThat(emissions[1], equalTo(DataState.Error<Unit>("Something is busted")))
        assertThat(emissions[2], equalTo(DataState.Loading<Unit>(DataState.Loading.LoadingState.Idle)))

        // Check file permissions is called once if API <= 28
        verify(checkFilePermissions, times(1)).invoke()

        // Never called since checkFilePermissions returns true
        verify(launchPermissionRequest, never()).invoke()
    }
}











