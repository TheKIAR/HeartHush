package com.hearthush.app.logic

/**
 * Role gate: the app starts in User (view-only) mode.
 * Admin (full edit) mode is unlocked with a password.
 * Default password: "admin123". Same hash scheme on every platform.
 */
class AuthService {
    private var adminMode = false

    fun isAdmin(): Boolean = adminMode

    fun verify(password: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PASSWORD)
        val ok = constantEquals(hash(password ?: ""), want)
        if (ok) adminMode = true
        return ok
    }

    fun logout() {
        adminMode = false
    }

    fun changePassword(current: String?, next: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PASSWORD)
        if (!constantEquals(hash(current ?: ""), want)) return false
        if (next == null || next.length < 4) return false
        prefsPut(HASH_KEY, hash(next))
        return true
    }

    companion object {
        private const val HASH_KEY = "admin_password_hash"
        private const val DEFAULT_PASSWORD = "admin123"

        fun hash(password: String): String {
            val md = sha256(("countdown-app|$password").encodeToByteArray())
            val sb = StringBuilder(md.size * 2)
            for (b in md) {
                val v = b.toInt() and 0xFF
                if (v < 16) sb.append('0')
                sb.append(v.toString(16))
            }
            return sb.toString()
        }

        private fun constantEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
            return diff == 0
        }
    }
}

expect fun sha256(data: ByteArray): ByteArray
