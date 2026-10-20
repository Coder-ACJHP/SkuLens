package com.coder.skulens.engine

import android.graphics.Bitmap

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
     *
     * If no distinct object is detected with sufficient confidence, it safely falls back
     * to a sensible crop or the original image without throwing exceptions or leaking memory.
     *
     * @param bitmap The source image to crop.
     * @return Tightly cropped Bitmap containing only the detected object.
     */
    fun cropTarget(bitmap: Bitmap): Bitmap

    override fun close() {
        // Optional cleanup hook for ML detector delegates/resources
    }
}
