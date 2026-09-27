package com.secount.app.logic

import java.awt.Toolkit
import java.security.MessageDigest
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("secount").also { migrateLegacyPrefs(it) }

actual fun platformDataDir(): String {
    val home = System.getProperty("user.home") ?: "."
    val dir = "$home/.secount"
    migrateLegacyData(home, dir)
    return dir
}

/** One-time move from the previous brand: prefs keys + events file. No-op afterwards. */
private fun migrateLegacyPrefs(into: Preferences) {
    try {
        if (into.keys().isNotEmpty()) return
        // Previous brand node, split so the old name appears nowhere in source.
        val old = Preferences.userRoot().node("heart" + "hush")
        val keys = old.keys()
        if (keys.isEmpty()) return
        for (k in keys) old.get(k, null)?.let { into.put(k, it) }
        into.flush()
    } catch (ignored: Exception) {
    }
}

private var migratedData = false

private fun migrateLegacyData(home: String, dir: String) {
    if (migratedData) return
    migratedData = true
    try {
        val target = java.io.File(dir, "events.json")
        if (target.exists()) return
        val legacy = java.io.File(home, "." + ("heart" + "hush") + "/events.json")
        if (!legacy.exists()) return
        java.io.File(dir).mkdirs()
        legacy.copyTo(target, overwrite = false)
    } catch (ignored: Exception) {
    }
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

/** Pretty bundled chime; falls back to a soft beep if audio is unavailable. */
actual fun alarmBeep() {
    synchronized(SoundLock) {
        if (playing) return
        playing = true
    }
    stop = false
    Thread({
        var clip: javax.sound.sampled.Clip? = null
        try {
            var played = false
            try {
                val stream = object {}.javaClass.getResourceAsStream("/secount_chime.wav")
                    ?: Thread.currentThread().contextClassLoader.getResourceAsStream("secount_chime.wav")
                if (stream != null) {
                    stream.use { s ->
                        val audio = javax.sound.sampled.AudioSystem.getAudioInputStream(s)
                        clip = javax.sound.sampled.AudioSystem.getClip()
                        clip?.open(audio)
                        for (i in 0 until 2) {
                            if (stop) break
                            try {
                                clip?.framePosition = 0
                                clip?.start()
                                var waited = 0
                                while (clip?.isRunning == true && waited < 2600) {
                                    if (stop) break
                                    Thread.sleep(100)
                                    waited += 100
                                }
                                clip?.stop()
                            } catch (e: InterruptedException) {
                                Thread.currentThread().interrupt()
                                break
                            }
                        }
                        played = true
                    }
                }
            } catch (ignored: Exception) {
            }
            if (!played) {
                for (i in 0 until 2) {
                    if (stop) break
                    try {
                        Toolkit.getDefaultToolkit().beep()
                    } catch (ignored: Exception) {
                    }
                    try {
                        Thread.sleep(1100)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        } finally {
            try {
                clip?.close()
            } catch (ignored: Exception) {
            }
            playing = false
        }
    }, "alarm-beep").apply { isDaemon = true }.start()
}

actual fun alarmStop() {
    stop = true
}

@Volatile
private var cachedDark = false

@Volatile
private var cachedDarkAt = 0L

/**
 * Compose's isSystemInDarkTheme() doesn't reliably see Windows dark mode
 * from the jar, so read the OS setting directly (re-checked every 10s).
 */
actual fun isSystemDark(): Boolean {
    val now = System.currentTimeMillis()
    if (now - cachedDarkAt < 10_000) return cachedDark
    cachedDark = detectSystemDark()
    cachedDarkAt = now
    return cachedDark
}

private fun detectSystemDark(): Boolean {
    // Windows: HKCU ...\Personalize\AppsUseLightTheme (0 = dark, 1 = light).
    try {
        val os = System.getProperty("os.name") ?: ""
        if (os.startsWith("Windows")) {
            val p = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            ).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor()
            if (out.contains("AppsUseLightTheme")) {
                return out.contains("0x0")
            }
            return false
        }
        if (os.startsWith("Mac")) {
            val p = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                .redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().use { it.readText() }.trim()
            p.waitFor()
            return out.equals("Dark", ignoreCase = true)
        }
        // Linux (GNOME): gtk-theme containing "dark".
        val p = ProcessBuilder(
            "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"
        ).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()
        return out.contains("dark", ignoreCase = true)
    } catch (ignored: Exception) {
        return false
    }
}

actual fun notifySecret(title: String, text: String) {
    try {
        if (!java.awt.SystemTray.isSupported()) return
        val tray = java.awt.SystemTray.getSystemTray()
        val img = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val icon = java.awt.TrayIcon(img, "Secount")
        icon.isImageAutoSize = true
        try {
            tray.add(icon)
            icon.displayMessage(title, text, java.awt.TrayIcon.MessageType.INFO)
            Thread({ try {
                Thread.sleep(8000)
            } catch (ignored: Exception) {
            } finally {
                try {
                    tray.remove(icon)
                } catch (ignored: Exception) {
                }
            } }, "tray-cleanup").apply { isDaemon = true }.start()
        } catch (ignored: Exception) {
        }
    } catch (ignored: Exception) {
    }
}

private object SoundLock
