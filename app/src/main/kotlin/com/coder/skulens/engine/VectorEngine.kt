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
            .setModelAssetPath("mobilenet_v3_large.tflite")
            .build()

        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setL2Normalize(true)
            .setQuantize(false)
            .build()

        embedder = ImageEmbedder.createFromOptions(context, options)
    }

    /** Vektör çıkarır; boyut modelle uyuşmazsa erken ve anlaşılır hata verir. */
    fun extractVector(bitmap: Bitmap): FloatArray? {
        // Güvenlik ağı: 1:1 vizör karesine kırp ve ≤512px'e boyutlandır
        val cropped = ImagePreprocessor.cropToSquare(bitmap)
        val scaled = ImagePreprocessor.resize(cropped)
        try {
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
        } finally {
            if (cropped !== bitmap && cropped !== scaled) cropped.recycle()
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    companion object {
        const val EMBEDDING_DIM = 1280L // mobilenet_v3_large (1280-dim); model değişirse güncelle
    }
}
