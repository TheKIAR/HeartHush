package com.hearthush.app.logic

import java.awt.Toolkit
import java.security.MessageDigest
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("hearthush")

actual fun platformDataDir(): String {
    val home = System.getProperty("user.home") ?: "."
    return "$home/.hearthush"
}

actual fun prefsGet(key: String): String? = prefs.get(key, null)

actual fun prefsPut(key: String, value: String) {
    prefs.put(key, value)
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
        try {
            for (i in 0 until 6) {
                if (stop) break
                try {
                    Toolkit.getDefaultToolkit().beep()
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
            playing = false
        }
    }, "alarm-beep").apply { isDaemon = true }.start()
}

actual fun alarmStop() {
    stop = true
}

private object SoundLock
