package com.coder.skulens.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.engine.VectorEngine
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder.StringOrder
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
        val trimmed = sku.trim().uppercase()
        if (trimmed.isEmpty()) return null
        return productBox.query(ProductEntity_.sku.equal(trimmed, StringOrder.CASE_INSENSITIVE))
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
        val trimmed = sku.trim().uppercase()
        if (trimmed.isEmpty()) return 0
        return productBox.query(ProductEntity_.sku.equal(trimmed, StringOrder.CASE_INSENSITIVE))
            .build()
            .use { it.count().toInt() }
    }

    fun saveProduct(sku: String, bitmap: Bitmap): SaveProductResult {
        val trimmed = sku.trim().uppercase()
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
            calibrateThreshold()
        } catch (e: Exception) {
            file.delete() // DB kaydı yoksa yetim dosya bırakma
            throw e
        }
        return SaveProductResult.Success(newCount = currentCount + 1)
    }

    fun saveProduct(sku: String, bitmaps: List<Bitmap>): SaveProductResult {
        val trimmed = sku.trim().uppercase()
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
            calibrateThreshold()
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

        val candidates = query.use { it.findWithScores() }
            .map {
                val entity = it.get()
                SearchResult(
                    sku = entity.sku,
                    similarity = 1f - it.score.toFloat(),
                    imagePath = File(imagesDir, entity.imageFile).absolutePath,
                    creationDate = entity.creationDate
                )
            }

        if (candidates.isNotEmpty()) {
            val preview = candidates.take(5).joinToString(", ") { "${it.sku}: ${"%.3f".format(it.similarity)}" }
            Log.d("ProductMatching", "Arama adayları (ilk 5): [$preview] | Eşik: $minSimilarity")
        } else {
            Log.d("ProductMatching", "Arama adayları: Veritabanında eşleşen komşu bulunamadı")
        }

        return candidates
            .filter { it.similarity >= minSimilarity }
            // Sonuçlar benzerliğe göre sıralı gelir; aynı SKU'dan ilk (en iyi eşleşen) fotoğraf kalır,
            // yani kartta gösterilen fotoğraf, sorguya en çok benzeyen kayıtlı fotoğraftır
            .distinctBy { it.sku }
            .take(maxResults)
    }

    data class CalibrationSummary(
        val sameSkuMin: Float,
        val sameSkuMean: Float,
        val diffSkuMax: Float,
        val diffSkuMean: Float,
        val recommendedThreshold: Float
    )

    /**
     * Veritabanındaki ürün vektörlerini inceleyerek ampirik eşik analizi yapar:
     * - Aynı SKU'ya ait farklı açıların kosinüs benzerliği (intra-class)
     * - Farklı SKU'lar arasındaki kosinüs benzerliği (inter-class)
     * İki dağılımın arasına denk gelen eşiği hesaplayarak loglar ve döndürür (tahmin yerine veri temelli).
     */
    fun calibrateThreshold(): CalibrationSummary? {
        val allEntities = productBox.all
        if (allEntities.size < 2) return null

        val sameSkuScores = mutableListOf<Float>()
        val diffSkuScores = mutableListOf<Float>()

        for (i in 0 until allEntities.size) {
            val e1 = allEntities[i]
            val v1 = e1.imageVector ?: continue
            for (j in i + 1 until allEntities.size) {
                val e2 = allEntities[j]
                val v2 = e2.imageVector ?: continue
                val sim = cosineSimilarity(v1, v2)
                if (e1.sku.equals(e2.sku, ignoreCase = true)) {
                    sameSkuScores.add(sim)
                } else {
                    diffSkuScores.add(sim)
                }
            }
        }

        if (sameSkuScores.isEmpty()) {
            Log.d("ProductMatching", "Kalibrasyon için aynı SKU'ya ait birden fazla açı henüz kaydedilmedi.")
            return null
        }

        val sameMin = sameSkuScores.minOrNull() ?: 0f
        val sameMean = sameSkuScores.average().toFloat()
        val diffMax = diffSkuScores.maxOrNull() ?: 0f
        val diffMean = if (diffSkuScores.isNotEmpty()) diffSkuScores.average().toFloat() else 0f

        val recommended = if (diffSkuScores.isNotEmpty()) {
            (sameMin + diffMax) / 2f
        } else {
            sameMin * 0.9f
        }.coerceIn(0.1f, 0.95f)

        val summary = CalibrationSummary(
            sameSkuMin = sameMin,
            sameSkuMean = sameMean,
            diffSkuMax = diffMax,
            diffSkuMean = diffMean,
            recommendedThreshold = recommended
        )

        Log.i(
            "ProductMatching",
            "Kalibrasyon Raporu: Aynı SKU [min=%.3f, ort=%.3f] | Farklı SKU [max=%.3f, ort=%.3f] -> Önerilen Eşik: %.3f"
                .format(sameMin, sameMean, diffMax, diffMean, recommended)
        )
        return summary
    }

    // Galeri (Photo Picker) kaynağı: decode + EXIF + 512px küçültme burada yapılır, IO thread'inde çağır
    fun saveProduct(sku: String, uri: Uri): SaveProductResult {
        val trimmed = sku.trim().uppercase()
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
                    .groupBy { it.sku.trim().uppercase() }
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
        const val DEFAULT_MIN_SIMILARITY = 0.55f

        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
            var dot = 0f
            var norm1 = 0f
            var norm2 = 0f
            val size = minOf(v1.size, v2.size)
            for (i in 0 until size) {
                dot += v1[i] * v2[i]
                norm1 += v1[i] * v1[i]
                norm2 += v2[i] * v2[i]
            }
            val denom = kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2)
            return if (denom > 0f) dot / denom else 0f
        }
    }
}
