package com.coder.skulens

import com.coder.skulens.data.ProductRepository
import com.coder.skulens.data.SaveProductResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals(1280L, com.coder.skulens.engine.VectorEngine.EMBEDDING_DIM)
        assertEquals(1280L, com.coder.skulens.data.ProductEntity.EMBEDDING_DIM)
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
