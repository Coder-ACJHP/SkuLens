package com.coder.skulens.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import com.coder.skulens.App
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

data class StorageVolumeInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val directory: File,
    val freeBytes: Long,
    val totalBytes: Long,
    val isRemovable: Boolean,
    val isCurrent: Boolean
) {
    val freeFormatted: String
        get() = formatBytes(freeBytes)

    val totalFormatted: String
        get() = formatBytes(totalBytes)

    val usedPercent: Float
        get() = if (totalBytes > 0) ((totalBytes - freeBytes).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f MB", mb)
            }
        }
    }
}

object StorageManager {

    fun getAvailableStorageVolumes(context: Context): List<StorageVolumeInfo> {
        val currentDir = AppPreferences.getActiveStorageDirectory(context).canonicalFile
        val volumes = mutableListOf<StorageVolumeInfo>()

        // 1. Dahili Uygulama Alanı (filesDir)
        val internalDir = context.filesDir.canonicalFile
        val internalStats = getStatFs(internalDir)
        volumes.add(
            StorageVolumeInfo(
                id = "internal",
                title = "Dahili Hafıza",
                subtitle = "Uygulama Dahili Belleği",
                directory = internalDir,
                freeBytes = internalStats.first,
                totalBytes = internalStats.second,
                isRemovable = false,
                isCurrent = currentDir.absolutePath == internalDir.absolutePath
            )
        )

        // 2. Harici Alanlar (Harici SD Kartlar, USB Bellekler / SSD)
        val externalDirs = context.getExternalFilesDirs(null)
        for ((index, extDir) in externalDirs.withIndex()) {
            if (extDir == null) continue
            val canonicalExt = extDir.canonicalFile
            // Eğer dahili alanla aynıysa veya zaten eklenmişse atla
            if (volumes.any { it.directory.absolutePath == canonicalExt.absolutePath }) continue

            val isRemovable = Environment.isExternalStorageRemovable(extDir)
            val stats = getStatFs(canonicalExt)

            val title = when {
                isRemovable -> "Harici Sürücü (SD Kart / SSD)"
                index == 0 -> "Paylaşımlı Dahili Depolama"
                else -> "Ek Depolama Birimi ($index)"
            }

            val subtitle = canonicalExt.absolutePath

            volumes.add(
                StorageVolumeInfo(
                    id = "ext_$index",
                    title = title,
                    subtitle = subtitle,
                    directory = canonicalExt,
                    freeBytes = stats.first,
                    totalBytes = stats.second,
                    isRemovable = isRemovable,
                    isCurrent = currentDir.absolutePath == canonicalExt.absolutePath
                )
            )
        }

        return volumes
    }

    private fun getStatFs(dir: File): Pair<Long, Long> {
        return try {
            if (!dir.exists()) dir.mkdirs()
            val stat = StatFs(dir.path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(available, total)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }

    /**
     * Veritabanı ve fotoğrafları kaynak dizinden hedef dizine kopyalar.
     * Güvenlik için önce mevcut BoxStore kapatılır.
     */
    fun migrateData(
        context: Context,
        targetDir: File,
        copyExistingData: Boolean,
        onProgress: (String) -> Unit
    ): Boolean {
        return try {
            val app = context.applicationContext as? App
            val sourceDir = AppPreferences.getActiveStorageDirectory(context).canonicalFile
            val targetCanonical = targetDir.canonicalFile

            if (sourceDir.absolutePath == targetCanonical.absolutePath) {
                return true
            }

            // ObjectBox bağlantısını güvenle kapat
            app?.closeBoxStore()

            if (copyExistingData) {
                onProgress("Veritabanı dosyaları kopyalanıyor...")
                val srcDbDir = File(sourceDir, "objectbox")
                val dstDbDir = File(targetCanonical, "objectbox")
                if (srcDbDir.exists() && srcDbDir.isDirectory) {
                    copyDirectory(srcDbDir, dstDbDir)
                }

                onProgress("Ürün fotoğrafları kopyalanıyor...")
                val srcImgDir = File(sourceDir, "product_images")
                val dstImgDir = File(targetCanonical, "product_images")
                if (srcImgDir.exists() && srcImgDir.isDirectory) {
                    copyDirectory(srcImgDir, dstImgDir)
                }
            }

            // Yeni yolu tercihlere kaydet
            if (targetCanonical.absolutePath == context.filesDir.canonicalFile.absolutePath) {
                AppPreferences.setCustomStoragePath(context, null)
            } else {
                AppPreferences.setCustomStoragePath(context, targetCanonical.absolutePath)
            }

            onProgress("Taşıma tamamlandı.")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            destination.mkdirs()
        }

        val files = source.listFiles() ?: return
        for (file in files) {
            val target = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, target)
            } else {
                copyFile(file, target)
            }
        }
    }

    @Throws(IOException::class)
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }
}
