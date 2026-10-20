package com.coder.skulens.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Stage 1 Object Detector & Tight Cropper.
 *
 * Implements [ITightCropper] using standard TensorFlow Lite [Interpreter] to locate clothing
 * articles, compute bounding box coordinates, and crop out extraneous background noise.
 *
 * Architecture:
 * - Compatible with standard Object Detection API / YOLO TFLite models without requiring
 *   proprietary MediaPipe Task metadata.
 * - Robust fallback: If model fails or no clothing is detected with sufficient confidence,
 *   safely returns the original bitmap without throwing exceptions or crashing the pipeline.
 */
class BoundingBoxCropper(
    private val context: Context,
    private val modelAssetPath: String = DEFAULT_DETECTOR_MODEL
) : ITightCropper {

    private var interpreter: Interpreter? = null
    private var inputHeight: Int = DETECTOR_INPUT_SIZE
    private var inputWidth: Int = DETECTOR_INPUT_SIZE

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelBuffer(context, modelAssetPath)
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                val tflite = Interpreter(modelBuffer, options)
                tflite.allocateTensors()

                val inputTensor = tflite.getInputTensor(0)
                val shape = inputTensor.shape()
                Log.i(
                    TAG,
                    "Loaded Stage 1 detector '$modelAssetPath': Input shape=[${shape.joinToString()}], DataType=${inputTensor.dataType()}"
                )

                if (shape.size == 4) {
                    inputHeight = shape[1]
                    inputWidth = shape[2]
                }

                interpreter = tflite
                Log.i(TAG, "Stage 1 ObjectDetector initialized successfully from asset: $modelAssetPath")
            } else {
                Log.w(
                    TAG,
                    "Detector asset '$modelAssetPath' not found or could not be loaded. " +
                        "Stage 1 will operate in pass-through fallback mode."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Stage 1 detector '$modelAssetPath': ${e.message}", e)
            interpreter = null
        }
    }

    override fun cropTarget(bitmap: Bitmap): Bitmap {
        val tflite = interpreter ?: return bitmap

        // Scale image to detector input size (typically 640x640)
        val scaled = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        } else {
            bitmap
        }

        return try {
            // Allocate uint8 buffer [1, H, W, 3]
            val bufferSize = 1 * inputHeight * inputWidth * 3
            val inputBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                order(ByteOrder.nativeOrder())
            }

            val pixels = IntArray(inputWidth * inputHeight)
            scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            // Fill standard NHWC RGB bytes
            for (pixel in pixels) {
                inputBuffer.put(((pixel shr 16) and 0xFF).toByte()) // R
                inputBuffer.put(((pixel shr 8) and 0xFF).toByte())  // G
                inputBuffer.put((pixel and 0xFF).toByte())         // B
            }
            inputBuffer.rewind()

            // Output tensor bindings for standard TFLite Object Detection API
            // Output 0: boxes [1, 25, 4] (ymin, xmin, ymax, xmax)
            // Output 1: classes [1, 25]
            // Output 2: scores [1, 25]
            // Output 3: num_detections [1]
            val outputMap = HashMap<Int, Any>()
            val boxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
            val classes = Array(1) { FloatArray(MAX_DETECTIONS) }
            val scores = Array(1) { FloatArray(MAX_DETECTIONS) }
            val numDetections = FloatArray(1)

            outputMap[0] = boxes
            outputMap[1] = classes
            outputMap[2] = scores
            outputMap[3] = numDetections

            tflite.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            val count = numDetections[0].toInt().coerceIn(0, MAX_DETECTIONS)
            var bestScore = -1.0f
            var bestBox: RectF? = null

            for (i in 0 until count) {
                val score = scores[0][i]
                if (score >= MIN_CONFIDENCE_THRESHOLD) {
                    val boxData = boxes[0][i] // [ymin, xmin, ymax, xmax]
                    val ymin = boxData[0].coerceIn(0f, 1f)
                    val xmin = boxData[1].coerceIn(0f, 1f)
                    val ymax = boxData[2].coerceIn(ymin, 1f)
                    val xmax = boxData[3].coerceIn(xmin, 1f)

                    val area = (xmax - xmin) * (ymax - ymin)
                    // Priority metric combining confidence and detected area
                    val candidateScore = score * (1f + area)
                    if (candidateScore > bestScore) {
                        bestScore = candidateScore
                        bestBox = RectF(xmin, ymin, xmax, ymax)
                    }
                }
            }

            if (bestBox != null) {
                Log.d(
                    TAG,
                    "Detected garment at [l=%.2f, t=%.2f, r=%.2f, b=%.2f] with score=%.2f"
                        .format(bestBox.left, bestBox.top, bestBox.right, bestBox.bottom, bestScore)
                )
                cropBoundingBox(bitmap, bestBox, PADDING_RATIO)
            } else {
                Log.d(TAG, "No clothing detected with score >= $MIN_CONFIDENCE_THRESHOLD. Using full frame.")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Stage 1 tight crop detection: ${e.message}", e)
            bitmap
        } finally {
            if (scaled !== bitmap) {
                scaled.recycle()
            }
        }
    }

    override fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ObjectDetector: ${e.message}")
        } finally {
            interpreter = null
        }
    }

    companion object {
        private const val TAG = "BoundingBoxCropper"
        const val DEFAULT_DETECTOR_MODEL = "clothing_detector.tflite"
        private const val DETECTOR_INPUT_SIZE = 640
        private const val MIN_CONFIDENCE_THRESHOLD = 0.25f
        private const val MAX_DETECTIONS = 25
        private const val PADDING_RATIO = 0.04f

        private fun loadModelBuffer(context: Context, modelPath: String): ByteBuffer? {
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
                    Log.e(TAG, "Cannot load detector model '$modelPath': ${fallbackError.message}")
                    null
                }
            }
        }

        fun cropBoundingBox(src: Bitmap, box: RectF, paddingRatio: Float = PADDING_RATIO): Bitmap {
            val srcWidth = src.width
            val srcHeight = src.height

            val isNormalized = box.left in 0f..1f && box.right in 0f..1f &&
                box.top in 0f..1f && box.bottom in 0f..1f &&
                (box.right <= 1.0f && box.bottom <= 1.0f) &&
                (srcWidth > 1 || srcHeight > 1)

            val rawLeft = if (isNormalized) box.left * srcWidth else box.left
            val rawTop = if (isNormalized) box.top * srcHeight else box.top
            val rawRight = if (isNormalized) box.right * srcWidth else box.right
            val rawBottom = if (isNormalized) box.bottom * srcHeight else box.bottom

            val boxLeft = minOf(rawLeft, rawRight)
            val boxRight = maxOf(rawLeft, rawRight)
            val boxTop = minOf(rawTop, rawBottom)
            val boxBottom = maxOf(rawTop, rawBottom)

            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop

            val padX = boxWidth * paddingRatio
            val padY = boxHeight * paddingRatio

            val clampedLeft = (boxLeft - padX).toInt().coerceIn(0, srcWidth - 1)
            val clampedTop = (boxTop - padY).toInt().coerceIn(0, srcHeight - 1)
            val clampedRight = (boxRight + padX).toInt().coerceIn(clampedLeft + 1, srcWidth)
            val clampedBottom = (boxBottom + padY).toInt().coerceIn(clampedTop + 1, srcHeight)

            val cropWidth = clampedRight - clampedLeft
            val cropHeight = clampedBottom - clampedTop

            if (clampedLeft == 0 && clampedTop == 0 && cropWidth == srcWidth && cropHeight == srcHeight) {
                return src
            }

            return Bitmap.createBitmap(src, clampedLeft, clampedTop, cropWidth, cropHeight)
        }
    }
}
