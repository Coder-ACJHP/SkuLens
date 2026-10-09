package com.coder.skulens.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.engine.VectorEngine
import io.objectbox.BoxStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProductRepository(
    boxStore: BoxStore,
    private val vectorEngine: VectorEngine,
    private val contentResolver: ContentResolver,
    private val imagesDir: File
) {
    private val productBox = boxStore.boxFor(ProductEntity::class.java)

    fun saveProduct(sku: String, bitmap: Bitmap): Boolean {
        // Tek hat: vektör ve saklanan fotoğraf aynı (≤512px) görüntüden gelir
        val prepared = ImagePreprocessor.resize(bitmap)
        val vector = vectorEngine.extractVector(prepared) ?: return false

        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                check(prepared.compress(Bitmap.CompressFormat.JPEG, 90, out)) { "JPEG yazılamadı" }
            }
            productBox.put(
                ProductEntity(sku = sku.trim(), imageFile = fileName, imageVector = vector)
            )
        } catch (e: Exception) {
            file.delete() // DB kaydı yoksa yetim dosya bırakma
            throw e
        }
        return true
    }

    fun searchProduct(
        bitmap: Bitmap,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): List<SearchResult> {
        val queryVector = vectorEngine.extractVector(bitmap) ?: return emptyList()

        // Aynı SKU'nun birden fazla fotoğrafı olabilir; tekilleştirme için fazla aday çek
        val query = productBox
            .query(ProductEntity_.imageVector.nearestNeighbors(queryVector, maxResults * 4))
            .build()

        return query.use { it.findWithScores() }
            .map {
                val entity = it.get()
                SearchResult(
                    sku = entity.sku,
                    similarity = 1f - it.score.toFloat(),
                    imagePath = File(imagesDir, entity.imageFile).absolutePath
                )
            }
            .filter { it.similarity >= minSimilarity }
            // Sonuçlar benzerliğe göre sıralı gelir; aynı SKU'dan ilk (en iyi eşleşen) fotoğraf kalır,
            // yani kartta gösterilen fotoğraf, sorguya en çok benzeyen kayıtlı fotoğraftır
            .distinctBy { it.sku }
            .take(maxResults)
    }

    // Galeri (Photo Picker) kaynağı: decode + EXIF + 512px küçültme burada yapılır, IO thread'inde çağır
    fun saveProduct(sku: String, uri: Uri): Boolean {
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri) ?: return false
        return saveProduct(sku, bitmap)
    }

    fun searchProduct(uri: Uri): List<SearchResult> {
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri) ?: return emptyList()
        return searchProduct(bitmap)
    }

    fun productCount(): Long = productBox.count()

    companion object {
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_MIN_SIMILARITY = 0.6f
    }
}
