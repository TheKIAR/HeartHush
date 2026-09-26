package com.hearthush.app.logic

/** Directory where events.json lives. */
expect fun platformDataDir(): String

/** Simple string key-value storage. */
expect fun prefsGet(key: String): String?
expect fun prefsPut(key: String, value: String)

/** Alarm sound. */
expect fun alarmBeep()
expect fun alarmStop()
