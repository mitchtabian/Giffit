package com.codingwithmitch.giffit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.interactors.GetAssetSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetAssetSizeTest {

    private lateinit var getAssetSize: GetAssetSize

    @Before
    fun init() {
        getAssetSize = GetAssetSize()
    }

    @Test
    fun verifyEmissions() = runTest {
        val context = RuntimeEnvironment.getApplication()

        // Save an image to cache
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.deal_with_it_sunglasses_default)
        val file = File(context.cacheDir, "someBitmap.png")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        // Confirm the file exists in the cache
        val internalStorageDirectory = context.cacheDir
        internalStorageDirectory.listFiles().forEach {
            System.out.println("name: ${it.name}")
            System.out.println("SIZE: ${it.length()}")
        }

        // Execute the use-case
        val uri = Uri.fromFile(file)
        System.out.println("uri: ${uri}")
        val emissions = getAssetSize.execute(
            contentResolver = context.contentResolver,
            uri = uri
        ).toList()

        var fileSize = 0
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            fileSize = inputStream.available()
            inputStream.close()
        }
        System.out.println("Expected size: ${fileSize}")

        assert(emissions[0] == DataState.Loading<Int>(DataState.Loading.LoadingState.Active()))
        System.out.println("SIZE: ${emissions[1]}")
        assert(emissions[0] == DataState.Data<Int>(fileSize))
//        assert(emissions[0] == DataState.Loading<Int>(DataState.Loading.LoadingState.Active()))

    }

}











