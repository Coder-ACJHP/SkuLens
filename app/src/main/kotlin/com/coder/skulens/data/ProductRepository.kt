package com.coder.skulens.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.coder.skulens.engine.IVisualSearchPipeline
import com.coder.skulens.engine.ImagePreprocessor
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

/**
 * Repository coordinating product persistence and visual search.
 *
 * Implements clean architecture by depending on [IVisualSearchPipeline] (Strategy Pattern)
 * instead of concrete ML models.
 */
class ProductRepository(
    boxStore: BoxStore,
    private val pipeline: IVisualSearchPipeline,
    private val contentResolver: ContentResolver,
    private val imagesDir: File
) {
    private val productBox = boxStore.boxFor(ProductEntity::class.java)

    init {
        migrateOutdatedVectorsIfNeeded()
    }

    /**
     * Graceful Schema / Dimension Migration:
     * Inspects stored product entities for dimension mismatches (e.g., migrating from
     * legacy 1280-dim MobileNet to 512-dim ViT).
     *
     * If an entity possesses an outdated vector dimension, this function attempts to re-embed
     * the product using its existing image file on disk. If the image cannot be read, the
     * stale vector is cleared to prevent HNSW index crashes.
     */
    private fun migrateOutdatedVectorsIfNeeded() {
        try {
            val all = productBox.all
            val targetDim = pipeline.embeddingDimension
            val toUpdate = mutableListOf<ProductEntity>()

            for (entity in all) {
                val vec = entity.imageVector
                if (vec != null && vec.size != targetDim) {
                    Log.w(
                        TAG,
                        "Found entity SKU=${entity.sku} with legacy vector dim=${vec.size} (expected $targetDim). Migrating..."
                    )
                    val imgFile = File(imagesDir, entity.imageFile)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        if (bitmap != null) {
                            val newVector = pipeline.extractEmbedding(bitmap)
                            entity.imageVector = newVector
                            toUpdate.add(entity)
                            bitmap.recycle()
                        } else {
                            entity.imageVector = null
                            toUpdate.add(entity)
                        }
                    } else {
                        entity.imageVector = null
                        toUpdate.add(entity)
                    }
                }
            }

            if (toUpdate.isNotEmpty()) {
                productBox.put(toUpdate)
                Log.i(TAG, "Successfully migrated ${toUpdate.size} product vectors to $targetDim-dim.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/migrating product vector dimensions: ${e.message}", e)
        }
    }

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

        // Downscale image to max standard resolution while preserving full aspect ratio
        val prepared = ImagePreprocessor.resize(bitmap)
        val vector = pipeline.extractEmbedding(prepared)
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
            file.delete()
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
                val vector = pipeline.extractEmbedding(prepared)
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

    /**
     * Executes visual search using the Two-Stage Pipeline and Max-Score Matching.
     *
     * Matching Flow:
     * 1. Extracts L2-normalized ViT embedding from the input image.
     * 2. Retrieves nearest neighbor candidate entities from ObjectBox HNSW index.
     * 3. Groups candidates by SKU (where each SKU has up to 3 stored angles).
     * 4. Evaluates [IVisualSearchPipeline.maxScoreMatch] across all stored angles of that SKU.
     * 5. The angle exhibiting the maximum score is chosen as the representative match.
     * 6. Filters by [minSimilarity] and orders results descending by match confidence.
     */
    fun searchProduct(
        bitmap: Bitmap,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): List<SearchResult> {
        val queryVector = pipeline.extractEmbedding(bitmap) ?: return emptyList()

        if (queryVector.size != pipeline.embeddingDimension) {
            Log.e(
                TAG,
                "Query vector dimension (${queryVector.size}) does not match pipeline (${pipeline.embeddingDimension})"
            )
            return emptyList()
        }

        // Pull sufficient neighbor candidates to cover multiple angles per SKU
        val candidateCount = (maxResults * MAX_ANGLES_PER_SKU * 3).coerceAtLeast(10)
        val candidateEntities = try {
            productBox.query(ProductEntity_.imageVector.nearestNeighbors(queryVector, candidateCount))
                .build()
                .use { it.find() }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing HNSW nearest neighbors search: ${e.message}", e)
            emptyList()
        }

        // Group entities by SKU to compare across all registered angles
        val groupedBySku = candidateEntities.groupBy { it.sku.trim().uppercase() }
        val evaluatedResults = mutableListOf<SearchResult>()

        for ((sku, angleEntities) in groupedBySku) {
            val validVectors = angleEntities.mapNotNull { it.imageVector }
            if (validVectors.isEmpty()) continue

            // Compute Max-Score across all registered angles for this SKU
            val maxScore = pipeline.maxScoreMatch(queryVector, validVectors)

            if (maxScore >= minSimilarity) {
                // Pick the specific entity that yielded the highest score
                val bestEntity = angleEntities.maxByOrNull { entity ->
                    entity.imageVector?.let { pipeline.calculateCosineSimilarity(queryVector, it) } ?: -1f
                } ?: angleEntities.first()

                evaluatedResults.add(
                    SearchResult(
                        sku = sku,
                        similarity = maxScore,
                        imagePath = File(imagesDir, bestEntity.imageFile).absolutePath,
                        creationDate = bestEntity.creationDate
                    )
                )
            }
        }

        // Sort descending by maximum angle match score
        evaluatedResults.sortByDescending { it.similarity }

        if (evaluatedResults.isNotEmpty()) {
            val preview = evaluatedResults.take(5).joinToString(", ") { "${it.sku}: ${"%.3f".format(it.similarity)}" }
            Log.d(TAG, "Search candidates (Top 5): [$preview] | Threshold: $minSimilarity")
        } else {
            Log.d(TAG, "No matching candidates found meeting threshold: $minSimilarity")
        }

        return evaluatedResults.take(maxResults)
    }

    data class CalibrationSummary(
        val sameSkuMin: Float,
        val sameSkuMean: Float,
        val diffSkuMax: Float,
        val diffSkuMean: Float,
        val recommendedThreshold: Float
    )

    /**
     * Performs empirical threshold analysis using the active pipeline's similarity function:
     * - Intra-class cosine similarity (same SKU, different angles)
     * - Inter-class cosine similarity (distinct SKUs)
     */
    fun calibrateThreshold(): CalibrationSummary? {
        val allEntities = productBox.all
        if (allEntities.size < 2) return null

        val sameSkuScores = mutableListOf<Float>()
        val diffSkuScores = mutableListOf<Float>()
        val targetDim = pipeline.embeddingDimension

        for (i in 0 until allEntities.size) {
            val e1 = allEntities[i]
            val v1 = e1.imageVector ?: continue
            if (v1.size != targetDim) continue

            for (j in i + 1 until allEntities.size) {
                val e2 = allEntities[j]
                val v2 = e2.imageVector ?: continue
                if (v2.size != targetDim) continue

                val sim = pipeline.calculateCosineSimilarity(v1, v2)
                if (e1.sku.equals(e2.sku, ignoreCase = true)) {
                    sameSkuScores.add(sim)
                } else {
                    diffSkuScores.add(sim)
                }
            }
        }

        if (sameSkuScores.isEmpty()) {
            Log.d(TAG, "Calibration requires multiple angles for at least one SKU.")
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
            TAG,
            "Calibration Report: Same SKU [min=%.3f, mean=%.3f] | Diff SKU [max=%.3f, mean=%.3f] -> Recommended Threshold: %.3f"
                .format(sameMin, sameMean, diffMax, diffMean, recommended)
        )
        return summary
    }

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
        private const val TAG = "ProductRepository"
        const val MAX_ANGLES_PER_SKU = 3
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_MIN_SIMILARITY = 0.55f

        /**
         * Static utility forwarding cosine similarity to guarantee backward compatibility
         * with existing unit tests.
         */
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
            return if (denom > 0f) (dot / denom).coerceIn(-1.0f, 1.0f) else 0f
        }
    }
}
