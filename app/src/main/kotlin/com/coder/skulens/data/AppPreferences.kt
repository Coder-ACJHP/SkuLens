package com.coder.skulens.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object AppPreferences {
    private const val PREFS_NAME = "siriuslens_settings"
    private const val KEY_PHOTO_RESIZE_SIZE = "photo_resize_size"
    private const val KEY_STORAGE_PATH = "storage_custom_path"
    private const val KEY_SEARCH_SIMILARITY = "search_min_similarity"

    const val DEFAULT_PHOTO_SIZE = 512
    const val DEFAULT_SEARCH_SIMILARITY = 0.75f

    val PHOTO_SIZE_PRESETS = listOf(384, 512, 768, 1024)

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSearchSimilarity(context: Context): Float {
        return prefs(context).getFloat(KEY_SEARCH_SIMILARITY, DEFAULT_SEARCH_SIMILARITY)
    }

    fun setSearchSimilarity(context: Context, similarity: Float) {
        val clamped = similarity.coerceIn(0.1f, 0.99f)
        prefs(context).edit().putFloat(KEY_SEARCH_SIMILARITY, clamped).apply()
        ProductRepository.activeMinSimilarity = clamped
    }

    fun getPhotoResizeSize(context: Context): Int {
        val size = prefs(context).getInt(KEY_PHOTO_RESIZE_SIZE, DEFAULT_PHOTO_SIZE)
        return if (size in PHOTO_SIZE_PRESETS) size else DEFAULT_PHOTO_SIZE
    }

    fun setPhotoResizeSize(context: Context, size: Int) {
        prefs(context).edit().putInt(KEY_PHOTO_RESIZE_SIZE, size).apply()
    }

    fun getCustomStoragePath(context: Context): String? {
        return prefs(context).getString(KEY_STORAGE_PATH, null)
    }

    fun setCustomStoragePath(context: Context, path: String?) {
        if (path.isNullOrBlank()) {
            prefs(context).edit().remove(KEY_STORAGE_PATH).apply()
        } else {
            prefs(context).edit().putString(KEY_STORAGE_PATH, path).apply()
        }
    }

    /**
     * Aktif veritabanı ve fotoğrafların depolandığı ana dizini döner.
     * Özel yol ayarlanmamışsa veya erişilemiyorsa context.filesDir döner.
     */
    fun getActiveStorageDirectory(context: Context): File {
        val customPath = getCustomStoragePath(context)
        if (!customPath.isNullOrBlank()) {
            val dir = File(customPath)
            if (dir.exists() || dir.mkdirs()) {
                return dir
            }
        }
        return context.filesDir
    }

    fun getActiveDbDirectory(context: Context): File {
        return File(getActiveStorageDirectory(context), "objectbox")
    }

    fun getActiveImagesDirectory(context: Context): File {
        return File(getActiveStorageDirectory(context), "product_images").apply { mkdirs() }
    }
}
