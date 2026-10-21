package com.coder.skulens.engine

import android.content.Context
import android.graphics.Bitmap
import com.coder.skulens.util.AppLogger
import kotlin.math.sqrt

/**
 * Concrete implementation of [IVisualSearchPipeline] assembling the two-stage visual search pipeline:
 *
 * Pipeline Flow:
 * ```
 * ┌──────────────┐     Stage 1 (Model 1)     ┌────────────────┐     Stage 2 (Model 2)     ┌───────────────────┐
 * │ Input Bitmap │ ────────────────────────> │ Tightly Cropped│ ─────────────────────────> │   L2-Normalized   │
 * │ (Full Frame) │  Tight Clothing Detection │     Garment    │   ViT Feature Extraction  │ 512-Dim Embedding │
 * └──────────────┘                           └────────────────┘   (Attention over patches) └───────────────────┘
 * ```
 *
 * Why Two Stages?
 * 1. Stage 1 (Crop): Removes confounding background noise, store fixtures, text labels, and hangers,
 *    preventing spurious activations.
 * 2. Stage 2 (ViT Embed): Computes deep multi-head self-attention over the isolated clothing patches,
 *    achieving superior discriminative power compared to shallow convolutional networks.
 *
 * Matching Strategy:
 * Uses Max-Score Cosine Similarity matching across multiple registered angles per product.
 */
class TwoStageVisionPipeline(
    private val cropper: ITightCropper,
    private val embedder: IImageEmbedder
) : IVisualSearchPipeline, AutoCloseable {

    /**
     * Secondary convenience constructor instantiating the default [BoundingBoxCropper]
     * and [ViTImageEmbedder] strategies using the Android [Context].
     */
    constructor(
        context: Context,
        detectorModel: String = BoundingBoxCropper.DEFAULT_DETECTOR_MODEL,
        embedderModel: String = ViTImageEmbedder.DEFAULT_MODEL_ASSET,
        dimension: Int = ViTImageEmbedder.DEFAULT_EMBEDDING_DIM
    ) : this(
        cropper = BoundingBoxCropper(context, detectorModel),
        embedder = ViTImageEmbedder(context, embedderModel, dimension)
    )

    override val embeddingDimension: Int
        get() = embedder.embeddingDimension

    /**
     * Executes the sequential two-stage vision pipeline:
     * 1. Invokes [cropper] to detect bounding box and remove background.
     * 2. Passes the isolated crop to [embedder] to extract the L2-normalized vector.
     * 3. Guarantees bitmap lifecycle management to prevent memory leaks.
     */
    override fun extractEmbedding(bitmap: Bitmap): FloatArray? {
        val cropped = try {
            cropper.cropTarget(bitmap)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage 1 Cropper failed, falling back to full frame", e)
            bitmap
        }

        return try {
            embedder.embed(cropped)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Stage 2 ViT Embedder failed to extract vector", e)
            null
        } finally {
            // Clean up intermediate cropped bitmap if a new allocation occurred
            if (cropped !== bitmap && !cropped.isRecycled) {
                cropped.recycle()
            }
        }
    }

    /**
     * Computes the Cosine Similarity between two feature vectors:
     *
     * Cosine Similarity Formula:
     *   sim(u, v) = (u . v) / ( ||u||_2 * ||v||_2 )
     *
     * Because [ViTImageEmbedder] enforces L2-normalization (||u|| = 1.0 and ||v|| = 1.0),
     * the denominator equals 1.0, and the score simplifies to the pure dot product:
     *   sim(u, v) = sum_{i=1}^D u_i * v_i
     *
     * We still evaluate norms defensively to gracefully handle any unnormalized vectors
     * from legacy data or mock sources.
     */
    override fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        val len = minOf(vectorA.size, vectorB.size)

        for (i in 0 until len) {
            val a = vectorA[i]
            val b = vectorB[i]
            dot += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denominator > 0.0) {
            ((dot / denominator).toFloat()).coerceIn(-1.0f, 1.0f)
        } else {
            0.0f
        }
    }

    /**
     * Max-Score Matching Algorithm across multi-angle product catalogs:
     *
     * Mathematical Definition:
     *   ProductScore(query, Product_k) = max_{v in Angles(Product_k)} [ cosine_similarity(query, v) ]
     *
     * Why Max-Score instead of Mean-Score / Average-Score?
     * In retail fashion, a product is typically cataloged from up to 3 distinct angles:
     *   1. Front view
     *   2. Back view
     *   3. Side / Detail / Label view
     *
     * If a customer takes a photo of a jacket's back design:
     * - Similarity to Front Angle: ~0.35 (different patterns/zippers)
     * - Similarity to Side Angle:  ~0.42
     * - Similarity to Back Angle:  ~0.94 (exact visual match)
     *
     * If we applied Mean-Score matching:
     *   Average = (0.35 + 0.42 + 0.94) / 3 = 0.57  -> (FAILS minSimilarity threshold, false negative)
     *
     * With Max-Score matching:
     *   Score = max(0.35, 0.42, 0.94) = 0.94        -> (STRONG MATCH, correctly ranked at #1)
     *
     * @param queryVector The feature vector extracted from the camera/gallery search image.
     * @param candidateVectors The list of up to 3 angle vectors stored for a product.
     * @return The highest similarity score found, or 0.0f if candidate list is empty.
     */
    override fun maxScoreMatch(queryVector: FloatArray, candidateVectors: List<FloatArray>): Float {
        if (candidateVectors.isEmpty()) return 0.0f

        var maxSimilarity = -1.0f
        for (candidate in candidateVectors) {
            val score = calculateCosineSimilarity(queryVector, candidate)
            if (score > maxSimilarity) {
                maxSimilarity = score
            }
        }

        return maxSimilarity.coerceAtLeast(0.0f)
    }

    override fun close() {
        cropper.close()
        embedder.close()
    }

    companion object {
        private const val TAG = "TwoStageVisionPipeline"

        /**
         * Standard embedding dimensionality for the two-stage pipeline.
         * Set to 512L (Fashion-CLIP ViT / standard CLIP ViT-B).
         * Used for ObjectBox @HnswIndex annotation.
         */
        const val EMBEDDING_DIM = 512L
    }
}
