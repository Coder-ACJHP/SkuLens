package com.coder.skulens

import android.app.Application
import com.coder.skulens.data.AppPreferences
import com.coder.skulens.data.MyObjectBox
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.engine.VectorEngine
import io.objectbox.BoxStore
import java.io.File

class App : Application() {
    lateinit var boxStore: BoxStore
        private set
    lateinit var repository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        initStorageAndEngine()
    }

    fun initStorageAndEngine() {
        // Tercih edilen fotoğraf boyutunu aktif ön işlemciye aktar
        ImagePreprocessor.activeMaxSide = AppPreferences.getPhotoResizeSize(this)

        val customPath = AppPreferences.getCustomStoragePath(this)
        val builder = MyObjectBox.builder().androidContext(this)
        if (!customPath.isNullOrBlank()) {
            val dbDir = File(customPath, "objectbox").apply { mkdirs() }
            builder.directory(dbDir)
        }
        boxStore = builder.build()

        val imagesDir = AppPreferences.getActiveImagesDirectory(this)
        repository = ProductRepository(boxStore, VectorEngine(this), contentResolver, imagesDir)
    }

    fun closeBoxStore() {
        if (::boxStore.isInitialized && !boxStore.isClosed) {
            boxStore.close()
        }
    }
}
