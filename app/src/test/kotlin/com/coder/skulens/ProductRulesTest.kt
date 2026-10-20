package com.coder.skulens

import android.graphics.Bitmap
import android.graphics.RectF
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.engine.BoundingBoxCropper
import com.coder.skulens.engine.TwoStageVisionPipeline
import com.coder.skulens.engine.ViTImageEmbedder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ProductRulesTest {

    @Test
    fun testMaxAnglesConstant() {
        assertEquals(3, ProductRepository.MAX_ANGLES_PER_SKU)
    }

    @Test
    fun testProductItemMultiImageProperties() {
        val item = com.coder.skulens.data.ProductItem(
            id = 1L,
            sku = "SKU-TEST",
            imagePath = "/path/1.jpg",
            creationDate = 1000L,
            additionalImagePaths = listOf("/path/2.jpg", "/path/3.jpg"),
            allIds = listOf(1L, 2L, 3L)
        )
        assertEquals(3, item.totalPhotosCount)
        assertEquals(3, item.allImagePaths.size)
        assertEquals(listOf("/path/1.jpg", "/path/2.jpg", "/path/3.jpg"), item.allImagePaths)
    }

    @Test
    fun testEmbeddingDimensionConstant() {
        // ViT (Fashion-CLIP) 512-dim contract
        assertEquals(512L, TwoStageVisionPipeline.EMBEDDING_DIM)
        assertEquals(512L, com.coder.skulens.data.ProductEntity.EMBEDDING_DIM)
        assertEquals(512, ViTImageEmbedder.DEFAULT_EMBEDDING_DIM)
    }

    @Test
    fun testDefaultMinSimilarity() {
        assertEquals(0.55f, ProductRepository.DEFAULT_MIN_SIMILARITY, 0.001f)
    }

    @Test
    fun testCosineSimilarityIdenticalAndOrthogonal() {
        val v1 = floatArrayOf(1f, 0f, 0f)
        val v2 = floatArrayOf(1f, 0f, 0f)
        val v3 = floatArrayOf(0f, 1f, 0f)

        assertEquals(1f, ProductRepository.cosineSimilarity(v1, v2), 0.0001f)
        assertEquals(0f, ProductRepository.cosineSimilarity(v1, v3), 0.0001f)
    }

    @Test
    fun testMaxScoreMatchingSelectsHighestAngleScore() {
        // Mock a 2-stage pipeline with dummy strategies for mathematical scoring
        val mockPipeline = object : com.coder.skulens.engine.IVisualSearchPipeline {
            override val embeddingDimension: Int = 3
            override fun extractEmbedding(bitmap: Bitmap): FloatArray? = null

            override fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float =
                ProductRepository.cosineSimilarity(vectorA, vectorB)

            override fun maxScoreMatch(queryVector: FloatArray, candidateVectors: List<FloatArray>): Float {
                if (candidateVectors.isEmpty()) return 0.0f
                return candidateVectors.maxOf { calculateCosineSimilarity(queryVector, it) }
            }
        }

        val query = floatArrayOf(0.9f, 0.1f, 0.0f)
        // Angle 1: Front (orthogonal / low similarity)
        val angle1 = floatArrayOf(0.0f, 1.0f, 0.0f)
        // Angle 2: Side (moderate similarity)
        val angle2 = floatArrayOf(0.6f, 0.6f, 0.0f)
        // Angle 3: Back (high visual similarity)
        val angle3 = floatArrayOf(0.92f, 0.08f, 0.0f)

        val candidateAngles = listOf(angle1, angle2, angle3)
        val maxScore = mockPipeline.maxScoreMatch(query, candidateAngles)

        val expectedMax = ProductRepository.cosineSimilarity(query, angle3)
        assertEquals(expectedMax, maxScore, 0.0001f)
        assertTrue("Max score must exceed 0.95", maxScore > 0.95f)
    }

    @Test
    fun testL2NormalizationProducesUnitVector() {
        val rawVector = floatArrayOf(3.0f, 4.0f, 0.0f) // Magnitude = 5.0
        val normalized = ViTImageEmbedder.normalizeL2(rawVector)

        assertEquals(3.0f / 5.0f, normalized[0], 0.0001f)
        assertEquals(4.0f / 5.0f, normalized[1], 0.0001f)
        assertEquals(0.0f, normalized[2], 0.0001f)

        // Euclidean norm of normalized vector must be exactly 1.0
        val sumSquares = normalized.sumOf { (it * it).toDouble() }
        assertEquals(1.0, sqrt(sumSquares), 0.0001)
    }

    @Test
    fun testSkuCaseInsensitiveNormalization() {
        val sku1 = "prd-0001"
        val sku2 = " PRD-0001"
        val sku3 = "PrD-0001 "
        val normalized1 = sku1.trim().uppercase()
        val normalized2 = sku2.trim().uppercase()
        val normalized3 = sku3.trim().uppercase()
        assertEquals("PRD-0001", normalized1)
        assertEquals(normalized1, normalized2)
        assertEquals(normalized2, normalized3)
    }

    @Test
    fun testPhotoSizePresetsContainDefaults() {
        assertEquals(512, com.coder.skulens.data.AppPreferences.DEFAULT_PHOTO_SIZE)
        assertTrue(com.coder.skulens.data.AppPreferences.PHOTO_SIZE_PRESETS.contains(512))
        assertEquals(listOf(384, 512, 768, 1024), com.coder.skulens.data.AppPreferences.PHOTO_SIZE_PRESETS)
    }

    @Test
    fun testStorageVolumeFormatBytes() {
        assertEquals("0 MB", com.coder.skulens.data.StorageVolumeInfo.formatBytes(0L))
        assertEquals("500 MB", com.coder.skulens.data.StorageVolumeInfo.formatBytes(500L * 1024 * 1024))
        assertEquals("2.0 GB", com.coder.skulens.data.StorageVolumeInfo.formatBytes(2L * 1024 * 1024 * 1024))
    }
}
