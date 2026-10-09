package com.coder.skulens

import android.app.Application
import io.objectbox.BoxStore
import java.io.File

class App : Application() {
    lateinit var boxStore: BoxStore
        private set
    lateinit var repository: ProductRepository
        private set

    override fun onCreate() {
        super.onCreate()
        boxStore = MyObjectBox.builder().androidContext(this).build()
        val imagesDir = File(filesDir, "product_images").apply { mkdirs() }
        repository = ProductRepository(boxStore, VectorEngine(this), contentResolver, imagesDir)
    }
}
