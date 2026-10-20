package com.coder.skulens.engine

import android.graphics.Bitmap

/**
 * Stage 2 Contract: Vision Transformer (ViT) Feature Extraction.
 *
 * Receives the tightly cropped image from Stage 1, handles model-specific tensor
 * transformations (resizing, standardization/normalization), executes inference,
 * and outputs an L2-normalized embedding vector.
 */
interface IImageEmbedder : AutoCloseable {

    /**
     * Fixed dimension size of the embedding vector (e.g., 512 for Fashion-CLIP / CLIP ViT-B,
     * 384 for DINOv2-Small, 768 for ViT-B/16).
     */
    val embeddingDimension: Int

    /**
     * Generates a high-quality embedding vector from the cropped image.
     *
     * @param croppedBitmap The tightly cropped target image from Stage 1.
     * @return L2-normalized float embedding vector, or null if feature extraction fails.
     */
    fun embed(croppedBitmap: Bitmap): FloatArray?

    override fun close() {
        // Optional cleanup hook for interpreter/embedder resources
    }
}
