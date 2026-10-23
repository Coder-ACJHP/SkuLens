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
        assertEquals(6, ProductRepository.MAX_ANGLES_PER_SKU)
    }

    @Test
    fun testIoUCalculation() {
        val boxA = com.coder.skulens.engine.GarmentBox(0f, 0f, 0.5f, 0.5f)
        val boxB = com.coder.skulens.engine.GarmentBox(0f, 0f, 0.5f, 0.5f)
        assertEquals(1.0f, BoundingBoxCropper.calculateIoU(boxA, boxB), 0.0001f)

        val disjointBox = com.coder.skulens.engine.GarmentBox(0.6f, 0.6f, 1f, 1f)
        assertEquals(0.0f, BoundingBoxCropper.calculateIoU(boxA, disjointBox), 0.0001f)

        // 50% overlap along one dimension
        val halfOverlap = com.coder.skulens.engine.GarmentBox(0.25f, 0f, 0.75f, 0.5f)
        // intersection: [0.25, 0, 0.5, 0.5] -> w=0.25, h=0.5, area=0.125
        // areaA = 0.25, areaB = 0.25, union = 0.5 - 0.125 = 0.375
        // IoU = 0.125 / 0.375 = 1/3
        assertEquals(1f / 3f, BoundingBoxCropper.calculateIoU(boxA, halfOverlap), 0.0001f)
    }

    @Test
    fun testNmsSuppression() {
        val highConfBox = com.coder.skulens.engine.GarmentBox(0.1f, 0.1f, 0.5f, 0.5f) to 0.95f
        val overlappingDuplicate = com.coder.skulens.engine.GarmentBox(0.12f, 0.11f, 0.51f, 0.49f) to 0.80f // high IoU with highConfBox
        val separateGarment = com.coder.skulens.engine.GarmentBox(0.6f, 0.1f, 0.95f, 0.5f) to 0.90f // separate dress

        val candidates = listOf(overlappingDuplicate, separateGarment, highConfBox)
        val filtered = BoundingBoxCropper.applyNms(candidates, iouThreshold = 0.45f)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.second == 0.95f })
        assertTrue(filtered.any { it.second == 0.90f })
    }

    @Test
    fun testHorizontalAwareNmsPreservesStackedDresses() {
        // Two dresses hanging next to each other on a rack (vertical, horizontally shifted, overlapping IoU > 0.45)
        val frontDress = com.coder.skulens.engine.GarmentBox(0.15f, 0.10f, 0.45f, 0.90f) to 0.90f // center 0.30, width 0.30
        val adjacentDress = com.coder.skulens.engine.GarmentBox(0.23f, 0.10f, 0.53f, 0.90f) to 0.70f // center 0.38, width 0.30

        // A duplicate box of the front dress (same horizontal position, shift ~3%)
        val duplicateFront = com.coder.skulens.engine.GarmentBox(0.16f, 0.11f, 0.46f, 0.89f) to 0.85f // center 0.31

        val candidates = listOf(duplicateFront, adjacentDress, frontDress)
        val filtered = BoundingBoxCropper.applyNms(candidates, iouThreshold = 0.45f)

        // Duplicate must be suppressed, but the adjacent hanging dress must be preserved!
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.second == 0.90f })
        assertTrue(filtered.any { it.second == 0.70f })
    }

    @Test
    fun testRackClusterDoesNotSuppressIndividualDresses() {
        // Enclosing rack box covering all dresses
        val macroRackBox = com.coder.skulens.engine.GarmentBox(0.05f, 0.05f, 0.95f, 0.95f) to 0.92f
        // 3 individual hanging dresses inside the rack
        val dress1 = com.coder.skulens.engine.GarmentBox(0.10f, 0.10f, 0.35f, 0.90f) to 0.88f
        val dress2 = com.coder.skulens.engine.GarmentBox(0.38f, 0.10f, 0.62f, 0.90f) to 0.86f
        val dress3 = com.coder.skulens.engine.GarmentBox(0.65f, 0.10f, 0.90f, 0.90f) to 0.84f

        val candidates = listOf(macroRackBox, dress1, dress2, dress3)
        val filtered = BoundingBoxCropper.applyNms(candidates, iouThreshold = 0.45f)

        // The enclosing macro rack must be removed, and all 3 individual dresses preserved in left-to-right order!
        assertEquals(3, filtered.size)
        assertEquals(0.10f, filtered[0].first.left, 0.001f)
        assertEquals(0.38f, filtered[1].first.left, 0.001f)
        assertEquals(0.65f, filtered[2].first.left, 0.001f)
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
        assertEquals(0.75f, ProductRepository.DEFAULT_MIN_SIMILARITY, 0.001f)
        assertEquals(0.75f, com.coder.skulens.data.AppPreferences.DEFAULT_SEARCH_SIMILARITY, 0.001f)
    }

    @Test
    fun testSearchResultMultiAngleProperties() {
        val result = com.coder.skulens.data.SearchResult(
            sku = "SKU-SHORTS",
            similarity = 0.91f,
            imagePath = "/path/angle1.jpg",
            creationDate = 1000L,
            allImagePaths = listOf("/path/angle1.jpg", "/path/angle2.jpg", "/path/angle3.jpg")
        )
        assertEquals("SKU-SHORTS", result.sku)
        assertEquals(3, result.allImagePaths.size)
        assertEquals("/path/angle1.jpg", result.imagePath)
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
