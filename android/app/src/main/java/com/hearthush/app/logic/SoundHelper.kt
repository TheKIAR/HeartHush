package com.hearthush.app.logic

import android.media.AudioManager
import android.media.ToneGenerator

/** Alarm beep on a background thread. */
object SoundHelper {

    @Volatile
    private var stop = false

    @Volatile
    private var playing = false

    @Synchronized
    fun playAlarm() {
        if (playing) return
        playing = true
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

    fun stop() {
        stop = true
    }
}
