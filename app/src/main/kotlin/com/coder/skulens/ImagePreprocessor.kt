package com.coder.skulens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.roundToInt

object ImagePreprocessor {

    const val MAX_SIDE = 512

    /** Galeri/dosya kaynağı. IO thread'inde çağır. Decode edilemezse null döner. */
    fun fromUri(resolver: ContentResolver, uri: Uri): Bitmap? {
        // 1) Sadece boyutları oku (bellek harcamaz)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 2) Hedefin en az 2 katı kalacak şekilde alt örnekleyerek decode et
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // 3) EXIF yönünü uygula, sonra kaliteli küçült
        val degrees = resolver.openInputStream(uri)?.use { exifDegrees(ExifInterface(it)) } ?: 0
        val oriented = decoded.rotate(degrees)
        if (oriented !== decoded) decoded.recycle()

        val result = resize(oriented)
        if (result !== oriented) oriented.recycle()
        return result
    }

    /**
     * Uzun kenar maxSide'dan büyükse en/boy oranını koruyarak küçültür,
     * değilse aynı nesneyi döner. Verilen src'yi recycle etmez.
     */
    fun resize(src: Bitmap, maxSide: Int = MAX_SIDE): Bitmap {
        if (max(src.width, src.height) <= maxSide) return src

        var current = src
        // Kademeli yarıya indirme: her adım en fazla 2x → aliasing yok
        while (max(current.width, current.height) / 2 >= maxSide) {
            val next = Bitmap.createScaledBitmap(
                current,
                (current.width / 2).coerceAtLeast(1),
                (current.height / 2).coerceAtLeast(1),
                true // bilinear filtre
            )
            if (current !== src) current.recycle()
            current = next
        }

        // Son adım: tam hedef boyuta (en/boy oranı korunur)
        val ratio = maxSide.toFloat() / max(current.width, current.height)
        val w = (current.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (current.height * ratio).roundToInt().coerceAtLeast(1)
        val result = Bitmap.createScaledBitmap(current, w, h, true)
        if (current !== src && current !== result) current.recycle()
        return result
    }

    private fun sampleSizeFor(longest: Int): Int {
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE * 2) sample *= 2
        return sample
    }

    private fun exifDegrees(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
