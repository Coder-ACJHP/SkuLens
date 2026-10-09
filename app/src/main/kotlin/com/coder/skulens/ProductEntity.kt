package com.coder.skulens

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.VectorDistanceType

@Entity
class ProductEntity(
    @Id var id: Long = 0,
    @Index var sku: String = "",

    // Fotoğraf dosyasının adı (yol değil); dosya filesDir/product_images/ altında durur
    var imageFile: String = "",

    // dimensions, .tflite modelinin çıktı boyutuyla birebir aynı olmalı
    @HnswIndex(dimensions = EMBEDDING_DIM, distanceType = VectorDistanceType.COSINE)
    var imageVector: FloatArray? = null
) {
    companion object {
        const val EMBEDDING_DIM = 1024L // mobilenet_v3_small; model değişirse güncelle
    }
}
