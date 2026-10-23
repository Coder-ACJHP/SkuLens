package com.coder.skulens.engine

import android.graphics.Bitmap
import android.graphics.RectF

data class GarmentBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = maxOf(0f, right - left)
    val height: Float get() = maxOf(0f, bottom - top)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

data class DetectedGarment(
    val box: GarmentBox,
    val score: Float,
    val crop: Bitmap
) {
    val rect: RectF get() = box.toRectF()
}

/**
 * Stage 1 Contract: Object Detection & Tight Cropping (Background Removal).
 *
 * Replaces naive center-cropping with intelligent object-aware bounding box cropping.
 * Responsible for locating clothing / fashion articles in the image and isolating
 * them from background clutter, hangers, store environments, and text overlays.
 */
interface ITightCropper : AutoCloseable {

    /**
     * Analyzes [bitmap], detects the primary object bounding box, and returns
     * a tightly cropped Bitmap focusing strictly on the subject.
     */
    fun cropTarget(bitmap: Bitmap): Bitmap

    /**
     * Detects all distinct clothing items in [bitmap], applies NMS and returns
     * coordinates and tightly cropped bitmaps for each detected garment.
     */
    fun detectObjects(
        bitmap: Bitmap,
        minConfidence: Float = 0.25f,
        iouThreshold: Float = 0.45f
    ): List<DetectedGarment> = emptyList()

    override fun close() {
        // Optional cleanup hook for ML detector delegates/resources
    }
}
