package com.secount.app.logic

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
        .getSharedPreferences("secount", Context.MODE_PRIVATE)
        .getString(key, null)

actual fun prefsPut(key: String, value: String) {
    (AppCtx.app ?: error("AppCtx not initialized"))
        .getSharedPreferences("secount", Context.MODE_PRIVATE)
        .edit().putString(key, value).apply()
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

/** Soft music-box chime (E–G#–B–E). Replaces the old harsh alarm tone. */
actual fun alarmBeep() {
    synchronized(SoundLock) {
        if (playing) return
        playing = true
    }
    stop = false
    Thread({
        var player: android.media.MediaPlayer? = null
        try {
            // Pretty bundled chime first.
            try {
                val ctx = AppCtx.app
                if (ctx != null) {
                    val resId = ctx.resources.getIdentifier(
                        "secount_chime", "raw", ctx.packageName
                    )
                    if (resId != 0) {
                        val afd = ctx.resources.openRawResourceFd(resId)
                        if (afd != null) {
                            player = android.media.MediaPlayer().apply {
                                setDataSource(
                                    afd.fileDescriptor, afd.startOffset, afd.length
                                )
                                afd.close()
                                setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()
                                )
                                prepare()
                            }
                            for (i in 0 until 2) {
                                if (stop) break
                                try {
                                    if (player?.isPlaying != true) player?.start()
                                    Thread.sleep(2400)
                                } catch (e: InterruptedException) {
                                    Thread.currentThread().interrupt()
                                    break
                                }
                            }
                            return@Thread
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            // Fallback: soft two-tone (much gentler than the old guard tone).
            var gen: ToneGenerator? = null
            try {
                gen = try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                } catch (e: Exception) {
                    null
                }
                for (i in 0 until 2) {
                    if (stop) break
                    try {
                        gen?.startTone(ToneGenerator.TONE_PROP_BEEP, 600)
                    } catch (ignored: Exception) {
                    }
                    try {
                        Thread.sleep(1100)
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
            }
        } finally {
            try {
                player?.release()
            } catch (ignored: Exception) {
            }
            playing = false
        }
    }, "alarm-beep").apply { isDaemon = true }.start()
}

actual fun alarmStop() {
    stop = true
}

actual fun notifySecret(title: String, text: String) {
    try {
        val ctx = AppCtx.app ?: return
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "secount_secret"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            var channel = try {
                mgr.getNotificationChannel(channelId)
            } catch (e: Exception) {
                null
            }
            if (channel == null) {
                channel = android.app.NotificationChannel(
                    channelId, "Secret messages",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                try {
                    val resId = ctx.resources.getIdentifier(
                        "secount_chime", "raw", ctx.packageName
                    )
                    if (resId != 0) {
                        val soundUri = android.net.Uri.parse(
                            "${android.content.ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/$resId"
                        )
                        val attrs = android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        channel.setSound(soundUri, attrs)
                    }
                } catch (ignored: Exception) {
                }
                try {
                    mgr.createNotificationChannel(channel)
                } catch (ignored: Exception) {
                }
            }
        }
        val intent = try {
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        } catch (e: Exception) {
            null
        }
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = try {
            if (intent != null) android.app.PendingIntent.getActivity(ctx, 0, intent, flags)
            else null
        } catch (e: Exception) {
            null
        }
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(ctx, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(ctx)
        }
        builder.setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
        if (pending != null) builder.setContentIntent(pending)
        try {
            mgr.notify(1001, builder.build())
        } catch (e: SecurityException) {
            // Notification permission not granted — in-app card still shows.
        }
    } catch (ignored: Exception) {
    }
}

private object SoundLock
