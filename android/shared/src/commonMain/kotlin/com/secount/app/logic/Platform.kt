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

/** True when the OS itself is in dark mode (used for the "System" appearance). */
expect fun isSystemDark(): Boolean

/** System notification for a D-day secret (Android posts one, desktop uses tray). */
expect fun notifySecret(title: String, text: String)

/** Copy text to the OS clipboard (pairing codes, backups, crash logs). */
expect fun copyToClipboard(text: String)

/** Current clipboard text, or null. */
expect fun getClipboardText(): String?

/** Open a photo picker; result is a stored photo filename (or null if cancelled).
 * Desktop blocks with a file dialog; Android launches the gallery and calls back later. */
expect fun pickPhotoFile(onResult: (String?) -> Unit)

/** Load a stored photo filename to an ImageBitmap for thumbnails, or null. */
expect fun loadPhotoBitmap(name: String): androidx.compose.ui.graphics.ImageBitmap?

/** Delete a stored photo filename. */
expect fun deletePhotoFile(name: String)

/** OS locale language code (e.g. "de"), used when language pref is System. */
expect fun systemLanguage(): String

/** Open a URL in the system browser (update download page). */
expect fun openUrl(url: String)

/** Push latest countdowns to the Android home widget snapshot (no-op on desktop). */
expect fun widgetRefresh(eventsJson: String)
