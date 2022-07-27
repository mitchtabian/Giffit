package com.codingwithmitch.giffit

import android.os.Build
import androidx.core.net.toFile
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.interactors.SaveGifToExternalStorage
import com.codingwithmitch.giffit.util.buildBitmap
import com.codingwithmitch.giffit.util.saveBytesToInternalStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SaveGifToExternalStorageTest {

    private lateinit var saveGifToExternalStorage: SaveGifToExternalStorage

    @Test
    fun `save to internal storage on API 29+`() = runTest {
        saveGifToExternalStorage = SaveGifToExternalStorage(
            mock {
                on { provideVersion() } doReturn 29
            }
        )
        val context = RuntimeEnvironment.getApplication()
        val contentResolver = context.contentResolver

        // Create a Bitmap
        val byteArray = buildBitmap(context.resources)

        // Save bitmap to internal storage
        val cacheProvider = RealCacheProvider(context)
        val uri = saveBytesToInternalStorage(
            cacheProvider = cacheProvider,
            contentResolver = contentResolver,
            bytes = byteArray
        )

        // Create mock checkFilePermissions so we can verify it's never called
        val checkFilePermissions: () -> Boolean = mock()

        // Create mock launchPermissionRequest so we can verify it's never called
        val launchPermissionRequest: () -> Unit = mock()

        // Save to external storage
        saveGifToExternalStorage.execute(
            contentResolver = contentResolver,
            cachedUri = uri,
            launchPermissionRequest = launchPermissionRequest,
            checkFilePermissions = checkFilePermissions
        ).toList()

        // CONTINUE...
    }
}











