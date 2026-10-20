package com.coder.skulens.data

import com.coder.skulens.engine.TwoStageVisionPipeline
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

    // dimensions, ViT modelinin çıktı boyutuyla birebir aynı olmalı (Fashion-CLIP 512-dim)
    @HnswIndex(dimensions = TwoStageVisionPipeline.EMBEDDING_DIM, distanceType = VectorDistanceType.COSINE)
    var imageVector: FloatArray? = null,

    var creationDate: Long = System.currentTimeMillis()
) {
    companion object {
        const val EMBEDDING_DIM = TwoStageVisionPipeline.EMBEDDING_DIM
    }
}
