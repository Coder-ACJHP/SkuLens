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
}
