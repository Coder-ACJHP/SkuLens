package com.coder.skulens

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

class VectorEngine(context: Context) {

    private val embedder: ImageEmbedder

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("mobilenet_v3_small.tflite")
            .build()

        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setQuantize(false)
            .build()

        embedder = ImageEmbedder.createFromOptions(context, options)
    }

    /** Vektör çıkarır; boyut modelle uyuşmazsa erken ve anlaşılır hata verir. */
    fun extractVector(bitmap: Bitmap): FloatArray? {
        // Güvenlik ağı: kaynaklar zaten küçültülmüş gelir, ≤512px ise dokunulmaz (idempotent)
        val scaled = ImagePreprocessor.resize(bitmap)
        val mpImage = BitmapImageBuilder(scaled).build()
        val vector = embedder.embed(mpImage)
            .embeddingResult()
            .embeddings()
            .firstOrNull()
            ?.floatEmbedding()

        check(vector == null || vector.size.toLong() == ProductEntity.EMBEDDING_DIM) {
            "Model ${vector?.size} boyutlu vektör üretti, EMBEDDING_DIM=${ProductEntity.EMBEDDING_DIM}. " +
                "ProductEntity içindeki değeri güncelle (ve uygulamayı kaldırıp yeniden kur)."
        }
        return vector
    }
}
