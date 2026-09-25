package com.hearthush.app.logic

import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Role gate: the app starts in User (view-only) mode.
 * Admin (full edit) mode is unlocked with a password.
 * Default password: "admin123". Same hash scheme as the desktop app.
 */
class AuthService(private val prefs: SharedPreferences) {

    private var adminMode = false

    fun isAdmin(): Boolean = adminMode

    fun verify(password: String?): Boolean {
        val want = prefs.getString(HASH_KEY, null) ?: hash(DEFAULT_PASSWORD)
        val ok = MessageDigest.isEqual(
            hash(password ?: "").toByteArray(Charsets.UTF_8),
            want.toByteArray(Charsets.UTF_8)
        )
        if (ok) adminMode = true
        return ok
    }

    fun logout() {
        adminMode = false
    }

    fun changePassword(current: String?, next: String?): Boolean {
        val want = prefs.getString(HASH_KEY, null) ?: hash(DEFAULT_PASSWORD)
        if (!MessageDigest.isEqual(
                hash(current ?: "").toByteArray(Charsets.UTF_8),
                want.toByteArray(Charsets.UTF_8)
            )
        ) return false
        if (next == null || next.length < 4) return false
        prefs.edit().putString(HASH_KEY, hash(next)).apply()
        return true
    }

    companion object {
        private const val HASH_KEY = "admin_password_hash"
        private const val DEFAULT_PASSWORD = "admin123"

        fun hash(password: String): String {
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val d = md.digest(("countdown-app|$password").toByteArray(Charsets.UTF_8))
                val sb = StringBuilder(d.size * 2)
                for (b in d) sb.append(String.format("%02x", b))
                return sb.toString()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }
    }
}
