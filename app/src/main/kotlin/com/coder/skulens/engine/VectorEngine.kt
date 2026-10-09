package com.coder.skulens.engine

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

        check(vector == null || vector.size.toLong() == EMBEDDING_DIM) {
            "Model ${vector?.size} boyutlu vektör üretti, EMBEDDING_DIM=$EMBEDDING_DIM. " +
                "Model değişirse EMBEDDING_DIM değerini güncelle (ve uygulamayı kaldırıp yeniden kur)."
        }
        return vector
    }

    companion object {
        const val EMBEDDING_DIM = 1024L // mobilenet_v3_small; model değişirse güncelle
    }
}
