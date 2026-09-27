package com.secount.app.logic

/** Directory where events.json lives. */
expect fun platformDataDir(): String

/** Simple string key-value storage. */
expect fun prefsGet(key: String): String?
expect fun prefsPut(key: String, value: String)

/** Minimal HTTPS transport (used for pairing + delivery sync). */
expect fun httpGet(url: String, timeoutMs: Int): String
expect fun httpPost(url: String, body: String, timeoutMs: Int): String

/** Current epoch seconds. */
expect fun nowSec(): Long

/** Raw SHA-256 digest. */
expect fun sha256(data: ByteArray): ByteArray

/** Alarm sound. */
expect fun alarmBeep()
expect fun alarmStop()

/** System notification for a D-day secret (Android posts one, desktop uses tray). */
expect fun notifySecret(title: String, text: String)
