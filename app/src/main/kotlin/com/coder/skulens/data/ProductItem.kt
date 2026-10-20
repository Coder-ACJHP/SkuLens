package com.coder.skulens.data

data class ProductItem(
    val id: Long,
    val sku: String,
    val imagePath: String,
    val creationDate: Long,
    val additionalImagePaths: List<String> = emptyList(),
    val allIds: List<Long> = listOf(id)
) {
    val totalPhotosCount: Int get() = 1 + additionalImagePaths.size
    val allImagePaths: List<String> get() = listOf(imagePath) + additionalImagePaths
}
