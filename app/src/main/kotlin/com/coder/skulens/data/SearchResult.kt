package com.coder.skulens.data

data class SearchResult(
    val sku: String,
    val similarity: Float,
    val imagePath: String // eşleşen kaydın fotoğrafının tam yolu
)
