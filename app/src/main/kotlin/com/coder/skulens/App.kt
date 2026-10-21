package com.coder.skulens

import android.app.Application
import com.coder.skulens.data.AppPreferences
import com.coder.skulens.data.MyObjectBox
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.engine.TwoStageVisionPipeline
import com.coder.skulens.util.AppLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        initCrashlytics()
        initStorageAndEngine()
    }

    private fun initCrashlytics() {
        // Crashlytics'i yalnızca release derlemelerinde aktif tutar, debug ortamında panoyu kirletmez
        runCatching {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        }
    }

    fun initStorageAndEngine() {
        // Update photo resize target size and visual search threshold from user preferences
        ImagePreprocessor.activeMaxSide = AppPreferences.getPhotoResizeSize(this)
        ProductRepository.activeMinSimilarity = AppPreferences.getSearchSimilarity(this)
        AppLogger.setCustomKey("photo_resize_side", ImagePreprocessor.activeMaxSide)
        AppLogger.setCustomKey("search_min_similarity", ProductRepository.activeMinSimilarity.toDouble())

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
            AppLogger.w("App", "ObjectBox store open failed (likely HNSW dimension update). Recreating database safely...", e)
            dbDir.deleteRecursively()
            dbDir.mkdirs()
            builder.build()
        }

        pipeline = TwoStageVisionPipeline(this)
        val imagesDir = AppPreferences.getActiveImagesDirectory(this)
        repository = ProductRepository(boxStore, pipeline, contentResolver, imagesDir)
        AppLogger.setCustomKey("storage_initialized", true)
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
