package com.coder.skulens.engine

import android.content.Context
import android.graphics.Bitmap
import com.coder.skulens.util.AppLogger
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Stage 2 Feature Extractor: Vision Transformer (ViT) Embedder.
 *
 * Implements [IImageEmbedder] using standard TensorFlow Lite [Interpreter] to transform the Stage 1
 * tightly cropped garment image into a dense 512-dimensional semantic representation vector.
 *
 * Why Standard TFLite Interpreter instead of MediaPipe Task Graph:
 * 1. Tensor Layout Agnostic: PyTorch / HuggingFace Vision Transformers (such as Fashion-CLIP and DINOv2)
 *    are exported in Channels-First NCHW tensor format: [Batch=1, Channels=3, Height=224, Width=224].
 *    MediaPipe Tasks vision graphs strictly reject NCHW models with dimension errors.
 * 2. Zero Metadata Requirement: Directly executes standard converted TFLite models without requiring
 *    proprietary Google MediaPipe Task metadata.
 * 3. High Performance: Directly leverages Android multi-threaded CPU / XNNPACK acceleration.
 */
class ViTImageEmbedder(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_MODEL_ASSET,
    override val embeddingDimension: Int = DEFAULT_EMBEDDING_DIM
) : IImageEmbedder {

    private var interpreter: Interpreter? = null
    private var isChannelsFirst: Boolean = true
    private var inputHeight: Int = INPUT_SIZE
    private var inputWidth: Int = INPUT_SIZE

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelBuffer(context, modelAssetPath)
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                    setCancellable(false)
                }
                val tflite = Interpreter(modelBuffer, options)
                tflite.allocateTensors()

                // Dynamically inspect model tensor input layout
                val inputTensor = tflite.getInputTensor(0)
                val shape = inputTensor.shape() // e.g. [1, 3, 224, 224] for NCHW, or [1, 224, 224, 3] for NHWC
                AppLogger.i(
                    TAG,
                    "Loaded ViT model '$modelAssetPath': Input shape=[${shape.joinToString()}], DataType=${inputTensor.dataType()}"
                )

                if (shape.size == 4) {
                    if (shape[1] == 3) {
                        isChannelsFirst = true // NCHW layout (Fashion-CLIP / PyTorch ViT standard)
                        inputHeight = shape[2]
                        inputWidth = shape[3]
                    } else if (shape[3] == 3) {
                        isChannelsFirst = false // NHWC layout (TensorFlow standard)
                        inputHeight = shape[1]
                        inputWidth = shape[2]
                    }
                }

                val outputTensor = tflite.getOutputTensor(0)
                val outShape = outputTensor.shape()
                AppLogger.i(TAG, "ViT Output shape=[${outShape.joinToString()}], DataType=${outputTensor.dataType()}")

                interpreter = tflite
                AppLogger.i(TAG, "ViT embedder successfully initialized for '$modelAssetPath' (dim=$embeddingDimension)")
            } else {
                AppLogger.w(TAG, "ViT model asset '$modelAssetPath' could not be loaded from assets.")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize ViT Interpreter for '$modelAssetPath': ${e.message}", e)
            interpreter = null
        }
    }

    override fun embed(croppedBitmap: Bitmap): FloatArray? {
        val tflite = interpreter ?: run {
            AppLogger.e(TAG, "Cannot embed image: ViT model '$modelAssetPath' interpreter is not loaded.")
            return null
        }

        // 1. Resize cropped garment to the exact input dimensions of the ViT backbone
        val scaled = if (croppedBitmap.width != inputWidth || croppedBitmap.height != inputHeight) {
            Bitmap.createScaledBitmap(croppedBitmap, inputWidth, inputHeight, true)
        } else {
            croppedBitmap
        }

        return try {
            // 2. Preprocess pixels into normalized IEEE 754 Float32 tensor buffer
            val inputBuffer = preprocessToTensorBuffer(
                bitmap = scaled,
                targetWidth = inputWidth,
                targetHeight = inputHeight,
                channelsFirst = isChannelsFirst,
                mean = CLIP_MEAN,
                std = CLIP_STD
            )

            // 3. Allocate output vector container [1, embeddingDimension]
            val outputArray = Array(1) { FloatArray(embeddingDimension) }

            // 4. Execute inference
            tflite.run(inputBuffer, outputArray)

            val rawVector = outputArray[0]

            // 5. Enforce L2 unit-sphere normalization
            normalizeL2(rawVector)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Inference error during ViT embedding extraction: ${e.message}", e)
            null
        } finally {
            if (scaled !== croppedBitmap) {
                scaled.recycle()
            }
        }
    }

    override fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error closing ViT Interpreter: ${e.message}")
        } finally {
            interpreter = null
        }
    }

    companion object {
        private const val TAG = "ViTImageEmbedder"
        const val DEFAULT_MODEL_ASSET = "fashion_clip.tflite"
        const val DEFAULT_EMBEDDING_DIM = 512
        const val INPUT_SIZE = 224

        // OpenAI CLIP / Fashion-CLIP standardization constants:
        // mu_c = [0.48145466, 0.4578275, 0.40821073]
        // sigma_c = [0.26862954, 0.26130258, 0.27577711]
        val CLIP_MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        val CLIP_STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

        // DINOv2 ImageNet standardization constants:
        val DINO_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val DINO_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        /**
         * Memory-maps a model file from Android assets with zero memory duplication.
         * Falls back safely to direct memory streaming if uncompressed asset mapping fails.
         */
        fun loadModelBuffer(context: Context, modelPath: String): ByteBuffer? {
            return try {
                val fileDescriptor = context.assets.openFd(modelPath)
                FileInputStream(fileDescriptor.fileDescriptor).use { stream ->
                    val channel = stream.channel
                    channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                }
            } catch (e: Exception) {
                try {
                    context.assets.open(modelPath).use { input ->
                        val bytes = input.readBytes()
                        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                        buffer.put(bytes)
                        buffer.rewind()
                        buffer
                    }
                } catch (fallbackError: Exception) {
                    AppLogger.e(TAG, "Cannot load model asset '$modelPath': ${fallbackError.message}", fallbackError)
                    null
                }
            }
        }

        /**
         * Converts an Android [Bitmap] into an IEEE 754 Float32 Tensor with exact channel
         * standardization and spatial layout.
         *
         * Mathematical Transformation:
         *   x_norm(y, x, c) = ( (Pixel(y, x, c) / 255.0) - mean[c] ) / std[c]
         *
         * Layout Handling:
         * - Channels-First (NCHW) [1, 3, H, W]:
         *     PyTorch standard used by Fashion-CLIP. Flattens all Red pixels for the entire grid,
         *     followed by all Green pixels, followed by all Blue pixels.
         * - Channels-Last (NHWC) [1, H, W, 3]:
         *     Standard TensorFlow layout. Interleaves R, G, B for each pixel (y, x).
         */
        fun preprocessToTensorBuffer(
            bitmap: Bitmap,
            targetWidth: Int = INPUT_SIZE,
            targetHeight: Int = INPUT_SIZE,
            channelsFirst: Boolean = true,
            mean: FloatArray = CLIP_MEAN,
            std: FloatArray = CLIP_STD
        ): ByteBuffer {
            val bufferSize = 1 * 3 * targetHeight * targetWidth * 4 // Float32 = 4 bytes
            val buffer = ByteBuffer.allocateDirect(bufferSize).apply {
                order(ByteOrder.nativeOrder())
            }

            val pixels = IntArray(targetWidth * targetHeight)
            bitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

            if (channelsFirst) {
                // NCHW: Planar channel representation
                // 1. Channel 0: Red
                for (pixel in pixels) {
                    val r = ((pixel shr 16) and 0xFF) / 255.0f
                    buffer.putFloat((r - mean[0]) / std[0])
                }
                // 2. Channel 1: Green
                for (pixel in pixels) {
                    val g = ((pixel shr 8) and 0xFF) / 255.0f
                    buffer.putFloat((g - mean[1]) / std[1])
                }
                // 3. Channel 2: Blue
                for (pixel in pixels) {
                    val b = (pixel and 0xFF) / 255.0f
                    buffer.putFloat((b - mean[2]) / std[2])
                }
            } else {
                // NHWC: Interleaved channel representation
                for (pixel in pixels) {
                    val r = ((pixel shr 16) and 0xFF) / 255.0f
                    val g = ((pixel shr 8) and 0xFF) / 255.0f
                    val b = (pixel and 0xFF) / 255.0f

                    buffer.putFloat((r - mean[0]) / std[0])
                    buffer.putFloat((g - mean[1]) / std[1])
                    buffer.putFloat((b - mean[2]) / std[2])
                }
            }

            buffer.rewind()
            return buffer
        }

        /**
         * L2 Euclidean Vector Normalization:
         *   ||v||_2 = sqrt( sum_{i=1}^D (v_i)^2 )
         *   v_norm_i = v_i / ( ||v||_2 + 1e-12 )
         */
        fun normalizeL2(vector: FloatArray): FloatArray {
            var sumSquares = 0.0
            for (value in vector) {
                sumSquares += (value * value).toDouble()
            }

            val norm = sqrt(sumSquares).toFloat()
            val epsilon = 1e-12f

            if (norm <= epsilon) {
                return vector.clone()
            }

            val normalized = FloatArray(vector.size)
            val invNorm = 1.0f / norm
            for (i in vector.indices) {
                normalized[i] = vector[i] * invNorm
            }
            return normalized
        }
    }
}
