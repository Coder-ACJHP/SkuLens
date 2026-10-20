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

sealed interface SaveProductResult {
    data class Success(val newCount: Int = 1) : SaveProductResult
    data class AlreadyExists(val existingProduct: ProductItem) : SaveProductResult
    data class Failure(val message: String? = null) : SaveProductResult
}

class ProductRepository(
    boxStore: BoxStore,
    private val vectorEngine: VectorEngine,
    private val contentResolver: ContentResolver,
    private val imagesDir: File
) {
    private val productBox = boxStore.boxFor(ProductEntity::class.java)

    fun findProductBySku(sku: String): ProductItem? {
        val trimmed = sku.trim()
        if (trimmed.isEmpty()) return null
        return productBox.query(ProductEntity_.sku.equal(trimmed))
            .build()
            .use { query ->
                query.findFirst()?.let { entity ->
                    ProductItem(
                        id = entity.id,
                        sku = entity.sku,
                        imagePath = File(imagesDir, entity.imageFile).absolutePath,
                        creationDate = entity.creationDate
                    )
                }
            }
    }

    fun countPhotosBySku(sku: String): Int {
        val trimmed = sku.trim()
        if (trimmed.isEmpty()) return 0
        return productBox.query(ProductEntity_.sku.equal(trimmed))
            .build()
            .use { it.count().toInt() }
    }

    fun saveProduct(sku: String, bitmap: Bitmap): SaveProductResult {
        val trimmed = sku.trim()
        val currentCount = countPhotosBySku(trimmed)
        if (currentCount >= MAX_ANGLES_PER_SKU) {
            val existing = findProductBySku(trimmed)
            return if (existing != null) {
                SaveProductResult.AlreadyExists(existing)
            } else {
                SaveProductResult.Failure("Maksimum $MAX_ANGLES_PER_SKU açı sınırına ulaşıldı.")
            }
        }

        // Tek hat: vektör ve saklanan fotoğraf aynı (≤512px) görüntüden gelir
        val prepared = ImagePreprocessor.resize(bitmap)
        val vector = vectorEngine.extractVector(prepared)
            ?: return SaveProductResult.Failure("Görsel işlenemedi")

        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                check(prepared.compress(Bitmap.CompressFormat.JPEG, 90, out)) { "JPEG yazılamadı" }
            }
            productBox.put(
                ProductEntity(sku = trimmed, imageFile = fileName, imageVector = vector)
            )
        } catch (e: Exception) {
            file.delete() // DB kaydı yoksa yetim dosya bırakma
            throw e
        }
        return SaveProductResult.Success(newCount = currentCount + 1)
    }

    fun saveProduct(sku: String, bitmaps: List<Bitmap>): SaveProductResult {
        val trimmed = sku.trim()
        if (bitmaps.isEmpty()) return SaveProductResult.Failure("En az bir fotoğraf gereklidir")
        val existing = findProductBySku(trimmed)
        if (existing != null) {
            return SaveProductResult.AlreadyExists(existing)
        }

        val toSave = bitmaps.take(MAX_ANGLES_PER_SKU)
        val filesCreated = mutableListOf<File>()
        val entitiesToInsert = mutableListOf<ProductEntity>()

        try {
            for (bitmap in toSave) {
                val prepared = ImagePreprocessor.resize(bitmap)
                val vector = vectorEngine.extractVector(prepared)
                    ?: throw IllegalStateException("Vektör çıkarılamadı")

                val fileName = "${UUID.randomUUID()}.jpg"
                val file = File(imagesDir, fileName)
                FileOutputStream(file).use { out ->
                    check(prepared.compress(Bitmap.CompressFormat.JPEG, 90, out)) { "JPEG yazılamadı" }
                }
                filesCreated.add(file)
                entitiesToInsert.add(ProductEntity(sku = trimmed, imageFile = fileName, imageVector = vector))
            }

            productBox.put(entitiesToInsert)
            return SaveProductResult.Success(newCount = entitiesToInsert.size)
        } catch (e: Exception) {
            filesCreated.forEach { it.delete() }
            throw e
        }
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
                    imagePath = File(imagesDir, entity.imageFile).absolutePath,
                    creationDate = entity.creationDate
                )
            }
            .filter { it.similarity >= minSimilarity }
            // Sonuçlar benzerliğe göre sıralı gelir; aynı SKU'dan ilk (en iyi eşleşen) fotoğraf kalır,
            // yani kartta gösterilen fotoğraf, sorguya en çok benzeyen kayıtlı fotoğraftır
            .distinctBy { it.sku }
            .take(maxResults)
    }

    // Galeri (Photo Picker) kaynağı: decode + EXIF + 512px küçültme burada yapılır, IO thread'inde çağır
    fun saveProduct(sku: String, uri: Uri): SaveProductResult {
        val trimmed = sku.trim()
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri)
            ?: return SaveProductResult.Failure("Görsel işlenemedi")
        return saveProduct(trimmed, bitmap)
    }

    fun decodeBitmapFromUri(uri: Uri): Bitmap? =
        ImagePreprocessor.fromUri(contentResolver, uri)

    fun searchProduct(uri: Uri): List<SearchResult> {
        val bitmap = ImagePreprocessor.fromUri(contentResolver, uri) ?: return emptyList()
        return searchProduct(bitmap)
    }

    fun productCount(): Long = productBox.count()

    fun getAllProducts(): List<ProductItem> {
        return productBox.query()
            .orderDesc(ProductEntity_.creationDate)
            .build()
            .use { query ->
                query.find()
                    .groupBy { it.sku }
                    .map { (sku, entities) ->
                        val first = entities.first()
                        ProductItem(
                            id = first.id,
                            sku = sku,
                            imagePath = File(imagesDir, first.imageFile).absolutePath,
                            creationDate = first.creationDate,
                            additionalImagePaths = entities.drop(1).map { File(imagesDir, it.imageFile).absolutePath },
                            allIds = entities.map { it.id }
                        )
                    }
            }
    }

    fun deleteProduct(product: ProductItem): Boolean {
        product.allIds.forEach { id ->
            val entity = productBox.get(id)
            if (entity != null) {
                val file = File(imagesDir, entity.imageFile)
                if (file.exists()) file.delete()
                productBox.remove(id)
            }
        }
        return true
    }

    fun deleteProduct(id: Long): Boolean {
        val entity = productBox.get(id) ?: return false
        val file = File(imagesDir, entity.imageFile)
        if (file.exists()) {
            file.delete()
        }
        return productBox.remove(id)
    }

    companion object {
        const val MAX_ANGLES_PER_SKU = 3
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_MIN_SIMILARITY = 0.6f
    }
}
