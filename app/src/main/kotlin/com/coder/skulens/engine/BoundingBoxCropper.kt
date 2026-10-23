package com.coder.skulens.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.coder.skulens.util.AppLogger
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

                if (shape.size == 4) {
                    inputHeight = shape[1]
                    inputWidth = shape[2]
                }

                val outTensor = tflite.getOutputTensor(0)
                val outShape = outTensor.shape()

                AppLogger.i(
                    TAG,
                    "Loaded Stage 1 detector '$modelAssetPath': input=[${shape.joinToString()}], out=[${outShape.joinToString()}]"
                )

                interpreter = tflite
            } else {
                AppLogger.w(
                    TAG,
                    "Detector asset '$modelAssetPath' not found or could not be loaded."
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize Stage 1 detector '$modelAssetPath': ${e.message}", e)
            interpreter = null
        }
    }

    var lastRunTelemetry: String = ""
        private set

    override fun cropTarget(bitmap: Bitmap): Bitmap {
        val detected = detectObjects(bitmap)
        if (detected.isEmpty()) return bitmap
        if (detected.size == 1) return detected[0].crop

        // Priority metric: favor central, well-proportioned vertical garments over wide multi-garment rack boxes
        val best = detected.maxByOrNull { item ->
            val distFromCenter = kotlin.math.hypot(item.box.centerX - 0.5f, item.box.centerY - 0.5f)
            val centerWeight = 1.0f - (distFromCenter * 0.6f).coerceIn(0f, 0.6f)
            val isVertical = item.box.height >= item.box.width * 0.75f
            val shapeWeight = if (isVertical) 1.25f else 0.85f
            val sizeWeight = if (item.box.area > 0.60f) 0.60f else 1.0f

            item.score * centerWeight * shapeWeight * sizeWeight
        }
        return best?.crop ?: bitmap
    }

    override fun detectObjects(
        bitmap: Bitmap,
        minConfidence: Float,
        iouThreshold: Float
    ): List<DetectedGarment> {
        val tflite = interpreter
        if (tflite == null) {
            AppLogger.w(TAG, "detectObjects aborted: interpreter is null")
            return emptyList()
        }

        val scaled = if (bitmap.width != inputWidth || bitmap.height != inputHeight) {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        } else {
            bitmap
        }

        return try {
            val rawCandidates = mutableListOf<Pair<GarmentBox, Float>>()

            // YOLOv8 Fashion Detector: Float32 [1, 640, 640, 3] -> [1, 8, 8400]
            val bufferSize = 1 * inputHeight * inputWidth * 3 * 4
            val inputBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                order(ByteOrder.nativeOrder())
            }

            val pixels = IntArray(inputWidth * inputHeight)
            scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            for (pixel in pixels) {
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
            inputBuffer.rewind()

            val outputArray = Array(1) { Array(8) { FloatArray(8400) } }
            val outputMap = HashMap<Int, Any>()
            outputMap[0] = outputArray

            tflite.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            val preds = outputArray[0]
            val allScores = mutableListOf<Float>()

            for (i in 0 until 8400) {
                val cx = preds[0][i]
                val cy = preds[1][i]
                val w = preds[2][i]
                val h = preds[3][i]
                val clothingScore = preds[4 + 2][i] // Sınıf 2: clothing
                allScores.add(clothingScore)

                if (clothingScore >= minConfidence) {
                    val xmin = ((cx - w / 2f) / inputWidth.toFloat()).coerceIn(0f, 1f)
                    val ymin = ((cy - h / 2f) / inputHeight.toFloat()).coerceIn(0f, 1f)
                    val xmax = ((cx + w / 2f) / inputWidth.toFloat()).coerceIn(xmin, 1f)
                    val ymax = ((cy + h / 2f) / inputHeight.toFloat()).coerceIn(ymin, 1f)

                    if (xmax > xmin && ymax > ymin) {
                        rawCandidates.add(GarmentBox(xmin, ymin, xmax, ymax) to clothingScore)
                    }
                }
            }

            val topScores = allScores.sortedDescending().take(5).map { "%.2f".format(it) }
            lastRunTelemetry = "YOLO Fashion: En Yüksek Skorlar: $topScores | Aday: ${rawCandidates.size}"
            AppLogger.i(TAG, "YOLO Fashion: topScores=$topScores, candidates=${rawCandidates.size}, minConfidence=$minConfidence")

            val nmsFiltered = applyNms(rawCandidates, iouThreshold)
            AppLogger.i(TAG, "Candidates before NMS: ${rawCandidates.size}, after NMS: ${nmsFiltered.size}")

            nmsFiltered.map { (box, score) ->
                val cropped = cropBoundingBox(bitmap, box.toRectF(), PADDING_RATIO)
                val resized = ImagePreprocessor.resize(cropped)
                if (resized !== cropped && !cropped.isRecycled && cropped !== bitmap) {
                    cropped.recycle()
                }
                DetectedGarment(box = box, score = score, crop = resized)
            }
        } catch (e: Exception) {
            lastRunTelemetry = "HATA: ${e.message}"
            AppLogger.e(TAG, "Error during Stage 1 multi-object detection: ${e.message}", e)
            emptyList()
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
            AppLogger.w(TAG, "Error closing ObjectDetector: ${e.message}")
        } finally {
            interpreter = null
        }
    }

    companion object {
        private const val TAG = "BoundingBoxCropper"
        const val DEFAULT_DETECTOR_MODEL = "clothing_detector.tflite"
        private const val DETECTOR_INPUT_SIZE = 640
        private const val MIN_CONFIDENCE_THRESHOLD = 0.20f
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
                    AppLogger.e(TAG, "Cannot load detector model '$modelPath': ${fallbackError.message}", fallbackError)
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

        fun calculateIoU(a: GarmentBox, b: GarmentBox): Float = calculateIoU(
            a.left, a.top, a.right, a.bottom,
            b.left, b.top, b.right, b.bottom
        )

        fun calculateIoU(a: RectF, b: RectF): Float = calculateIoU(
            a.left, a.top, a.right, a.bottom,
            b.left, b.top, b.right, b.bottom
        )

        fun calculateIoU(
            aLeft: Float, aTop: Float, aRight: Float, aBottom: Float,
            bLeft: Float, bTop: Float, bRight: Float, bBottom: Float
        ): Float {
            val intersectionLeft = maxOf(aLeft, bLeft)
            val intersectionTop = maxOf(aTop, bTop)
            val intersectionRight = minOf(aRight, bRight)
            val intersectionBottom = minOf(aBottom, bBottom)
            val width = maxOf(0f, intersectionRight - intersectionLeft)
            val height = maxOf(0f, intersectionBottom - intersectionTop)
            val intersectionArea = width * height
            val areaA = (aRight - aLeft) * (aBottom - aTop)
            val areaB = (bRight - bLeft) * (bBottom - bTop)
            val unionArea = areaA + areaB - intersectionArea
            return if (unionArea <= 0f) 0f else intersectionArea / unionArea
        }

        fun applyNms(
            boxes: List<Pair<GarmentBox, Float>>,
            iouThreshold: Float = 0.45f,
            horizontalShiftThreshold: Float = 0.10f
        ): List<Pair<GarmentBox, Float>> {
            val sorted = boxes.sortedByDescending { it.second }
            val selected = mutableListOf<Pair<GarmentBox, Float>>()
            for (candidate in sorted) {
                val candidateBox = candidate.first
                val isSuppressed = selected.any { (selectedBox, _) ->
                    val iou = calculateIoU(selectedBox, candidateBox)
                    if (iou <= 0f) return@any false

                    // Almost identical duplicate box
                    if (iou > 0.85f) return@any true

                    val avgWidth = (selectedBox.width + candidateBox.width) / 2f
                    val horizontalShift = kotlin.math.abs(selectedBox.centerX - candidateBox.centerX)
                    val shiftRatio = if (avgWidth > 0.001f) horizontalShift / avgWidth else 0f

                    // Hanging garments typically have height > width * 0.8f
                    val isVerticalGarment = selectedBox.height > selectedBox.width * 0.8f ||
                        candidateBox.height > candidateBox.width * 0.8f
                    val isDistinctHangingGarment = isVerticalGarment && shiftRatio >= horizontalShiftThreshold

                    // Check containment: only suppress if the candidate is a tiny sub-feature (pocket/collar/detail)
                    val intersectionLeft = maxOf(selectedBox.left, candidateBox.left)
                    val intersectionTop = maxOf(selectedBox.top, candidateBox.top)
                    val intersectionRight = minOf(selectedBox.right, candidateBox.right)
                    val intersectionBottom = minOf(selectedBox.bottom, candidateBox.bottom)
                    val interW = maxOf(0f, intersectionRight - intersectionLeft)
                    val interH = maxOf(0f, intersectionBottom - intersectionTop)
                    val intersectionArea = interW * interH
                    val minArea = minOf(selectedBox.area, candidateBox.area)
                    val containment = if (minArea > 0f) intersectionArea / minArea else 0f

                    val smallerBox = if (selectedBox.area < candidateBox.area) selectedBox else candidateBox
                    val isSubFeature = smallerBox.area < 0.08f || smallerBox.height < 0.22f

                    if (containment > 0.85f && isSubFeature) {
                        return@any true
                    }

                    if (iou > iouThreshold) {
                        !isDistinctHangingGarment
                    } else {
                        false
                    }
                }
                if (!isSuppressed) {
                    selected.add(candidate)
                }
            }

            // Remove macro-cluster boxes if multiple individual garments exist inside them
            val nonClusterBoxes = if (selected.size > 1) {
                selected.filterNot { (box, _) ->
                    val isWideMacroBox = box.area > 0.35f && box.width > 0.40f
                    if (!isWideMacroBox) return@filterNot false
                    val containedGarmentsCount = selected.count { (other, _) ->
                        other !== box &&
                            other.centerX in box.left..box.right &&
                            other.centerY in box.top..box.bottom &&
                            (other.height > other.width * 0.75f) &&
                            other.area < box.area * 0.65f
                    }
                    containedGarmentsCount >= 2
                }
            } else {
                selected
            }

            // Sort spatially from left to right (natural order for hanging clothes)
            return nonClusterBoxes.sortedBy { it.first.left }
        }
    }
}
