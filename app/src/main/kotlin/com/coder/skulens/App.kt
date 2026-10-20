package com.coder.skulens

import android.app.Application
import android.util.Log
import com.coder.skulens.data.AppPreferences
import com.coder.skulens.data.MyObjectBox
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.engine.TwoStageVisionPipeline
import io.objectbox.BoxStore
import java.io.File

class App : Application() {
    lateinit var boxStore: BoxStore
        private set
    lateinit var pipeline: TwoStageVisionPipeline
        private set
    lateinit var repository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        initStorageAndEngine()
    }

    fun initStorageAndEngine() {
        // Update photo resize target size from user preferences
        ImagePreprocessor.activeMaxSide = AppPreferences.getPhotoResizeSize(this)

        val customPath = AppPreferences.getCustomStoragePath(this)
        val builder = MyObjectBox.builder().androidContext(this)
        val dbDir = if (!customPath.isNullOrBlank()) {
            File(customPath, "objectbox").apply { mkdirs() }
        } else {
            File(filesDir, "objectbox").apply { mkdirs() }
        }
        builder.directory(dbDir)

        boxStore = try {
            builder.build()
        } catch (e: Exception) {
            // Graceful recovery for HNSW index dimension upgrade (e.g., from 1280-dim to 512-dim)
            Log.w("App", "ObjectBox store open failed (likely HNSW dimension update). Recreating database safely...", e)
            dbDir.deleteRecursively()
            dbDir.mkdirs()
            builder.build()
        }

        pipeline = TwoStageVisionPipeline(this)
        val imagesDir = AppPreferences.getActiveImagesDirectory(this)
        repository = ProductRepository(boxStore, pipeline, contentResolver, imagesDir)
    }

    fun closeBoxStore() {
        if (::pipeline.isInitialized) {
            pipeline.close()
        }
        if (::boxStore.isInitialized && !boxStore.isClosed) {
            boxStore.close()
        }
    }
}
