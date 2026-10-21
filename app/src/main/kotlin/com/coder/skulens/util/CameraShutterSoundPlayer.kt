package com.coder.skulens.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Deklanşör seslerinin çalınmasını ve ses profili kontrollerini yöneten sözleşme.
 * SOLID ilkelerinden Single Responsibility (SRP) ve Dependency Inversion (DIP) prensiplerini sağlar.
 */
interface IShutterSoundPlayer : AutoCloseable {
    fun play()
    fun release()
}

/**
 * Android [MediaActionSound] ve [AudioManager] kullanarak deklanşör seslerini yöneten somut sınıf.
 * Cihazın sessiz/titreşim modlarını, sistem ses seviyesini ve bölgesel yasal zorunlulukları otomatik denetler.
 */
class CameraShutterSoundPlayer(private val context: Context) : IShutterSoundPlayer {

    private val sound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
    }

    override fun play() {
        if (shouldPlaySound()) {
            sound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    override fun release() {
        sound.release()
    }

    override fun close() {
        release()
    }

    /**
     * Cihazın ses profiline göre deklanşör sesinin çalınıp çalınmayacağını belirler.
     * Cihaz sessiz veya titreşim modundayken veya sistem sesi sıfırken ses çalınmasını engeller.
     */
    private fun shouldPlaySound(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        // Cihaz sessiz veya titreşim modundaysa ses çalma
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            // Yasal olarak deklanşör sesi zorunlu olan ülke/modellerde (Japonya, Güney Kore vb.) kontrol et
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                runCatching { MediaActionSound.mustPlayShutterSound() }.getOrDefault(false)
            ) {
                return true
            }
            return false
        }

        // Normal modda iken sistem ses düzeyi 0 ise ses çalma
        val systemVolume = audioManager?.getStreamVolume(AudioManager.STREAM_SYSTEM) ?: 0
        return systemVolume > 0
    }
}

/**
 * Jetpack Compose yaşam döngüsüne bağlı bir [IShutterSoundPlayer] örneği sağlar ve
 * Composable ekrandan ayrıldığında kaynakları otomatik serbest bırakır.
 */
@Composable
fun rememberShutterSoundPlayer(): IShutterSoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember { CameraShutterSoundPlayer(context) }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
    return player
}
