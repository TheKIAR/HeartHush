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

actual fun nowSec(): Long = System.currentTimeMillis() / 1000

actual fun httpGet(url: String, timeoutMs: Int): String {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    try {
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.setRequestProperty("Accept", "application/json")
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        // ntfy keeps the stream open; a read timeout just ends our poll.
        val out = StringBuilder()
        try {
            stream.bufferedReader(Charsets.UTF_8).use { r ->
                while (true) {
                    val line = try {
                        r.readLine()
                    } catch (e: java.net.SocketTimeoutException) {
                        break
                    }
                    if (line == null) break
                    out.append(line).append('\n')
                    if (out.length > 200_000) break
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            // partial content is fine
        }
        if (code !in 200..299) throw java.io.IOException("HTTP $code")
        return out.toString()
    } finally {
        c.disconnect()
    }
}

actual fun httpPost(url: String, body: String, timeoutMs: Int): String {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    try {
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.requestMethod = "POST"
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val resp = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw java.io.IOException("HTTP $code: $resp")
        return resp
    } finally {
        c.disconnect()
    }
}

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
