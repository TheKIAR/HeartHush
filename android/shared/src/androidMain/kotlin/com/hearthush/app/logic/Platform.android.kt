package com.hearthush.app.logic

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import java.security.MessageDigest

object AppCtx {
    var app: Context? = null
}

actual fun platformDataDir(): String =
    (AppCtx.app ?: error("AppCtx not initialized")).filesDir.absolutePath

actual fun prefsGet(key: String): String? =
    (AppCtx.app ?: error("AppCtx not initialized"))
        .getSharedPreferences("hearthush", Context.MODE_PRIVATE)
        .getString(key, null)

actual fun prefsPut(key: String, value: String) {
    (AppCtx.app ?: error("AppCtx not initialized"))
        .getSharedPreferences("hearthush", Context.MODE_PRIVATE)
        .edit().putString(key, value).apply()
}

actual fun sha256(data: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(data)

@Volatile
private var stop = false

@Volatile
private var playing = false

actual fun alarmBeep() {
    synchronized(SoundLock) {
        if (playing) return
        playing = true
    }
    stop = false
    Thread({
        var gen: ToneGenerator? = null
        try {
            gen = try {
                ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (e: Exception) {
                null
            }
            for (i in 0 until 6) {
                if (stop) break
                try {
                    gen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
                } catch (ignored: Exception) {
                }
                try {
                    Thread.sleep(900)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        } finally {
            try {
                gen?.release()
            } catch (ignored: Exception) {
            }
            playing = false
        }
    }, "alarm-beep").apply { isDaemon = true }.start()
}

actual fun alarmStop() {
    stop = true
}

private object SoundLock
