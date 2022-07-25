package com.codingwithmitch.giffit

import androidx.core.net.toUri
import com.codingwithmitch.giffit.domain.DataState
import com.codingwithmitch.giffit.domain.RealCacheProvider
import com.codingwithmitch.giffit.interactors.ClearCachedFiles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClearCachedFilesTest {

    private lateinit var clearCachedFiles: ClearCachedFiles
    private val cacheProvider = RealCacheProvider(RuntimeEnvironment.getApplication())

    @Before
    fun init() {
        clearCachedFiles = ClearCachedFiles(cacheProvider)
    }

    @Test
    fun verifyCacheCleared() = runTest {
        val context = RuntimeEnvironment.getApplication()

        // Insert a bunch of files into the cache
        repeat(5) {
            val file = File.createTempFile("${UUID.randomUUID()}.txt", null, cacheProvider.gifCache())
            val uri = file.toUri()
            context.contentResolver.openOutputStream(uri)?.let { os ->
                os.write(ByteArray(1))
                os.flush()
                os.close()
            }
        }

        // Confirm those 5 files exist in the cache
        val internalStorageDirectory = cacheProvider.gifCache()
        var files = internalStorageDirectory.listFiles()
        repeat(5) {
            assert(files[it].exists())
        }

        // Execute use-case and confirm the files are deleted
        clearCachedFiles.execute().toList()

        files = internalStorageDirectory.listFiles()
        assert(files.isEmpty())
    }

    @Test
    fun verifyFlowEmissions() = runTest {
        val emissions = clearCachedFiles.execute().toList()
        assert(emissions[0] == DataState.Loading<Unit>(DataState.Loading.LoadingState.Active()))
        assert(emissions[1] == DataState.Data(Unit))
        assert(emissions[2] == DataState.Loading<Unit>(DataState.Loading.LoadingState.Idle))
    }
}











