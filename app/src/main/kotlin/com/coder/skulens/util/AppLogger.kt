package com.coder.skulens.util

import android.util.Log
import com.coder.skulens.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * SkuLens merkezi loglama ve teşhis yönetim sınıfı.
 *
 * Debug derlemelerinde tüm logları Logcat'e yönlendirir ve Crashlytics'e gönderim yapmaz.
 * Release derlemelerinde ise Logcat'in yanı sıra Crashlytics'e breadcrumb loglarını ve
 * non-fatal hataları iletir.
 */
object AppLogger {

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        if (!BuildConfig.DEBUG) {
            recordBreadcrumb("[$tag] $message")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }

        if (!BuildConfig.DEBUG) {
            recordBreadcrumb("[$tag] WARN: $message")
            if (throwable != null) {
                recordNonFatal(throwable)
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }

        if (!BuildConfig.DEBUG) {
            recordBreadcrumb("[$tag] ERROR: $message")
            if (throwable != null) {
                recordNonFatal(throwable)
            }
        }
    }

    fun setCustomKey(key: String, value: String) {
        if (!BuildConfig.DEBUG) {
            runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        if (!BuildConfig.DEBUG) {
            runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        }
    }

    fun setCustomKey(key: String, value: Int) {
        if (!BuildConfig.DEBUG) {
            runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        }
    }

    fun setCustomKey(key: String, value: Double) {
        if (!BuildConfig.DEBUG) {
            runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        }
    }

    private fun recordBreadcrumb(message: String) {
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
    }

    private fun recordNonFatal(throwable: Throwable) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
    }
}
