package countdown

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.prefs.Preferences

/**
 * Role gate: the app starts in User (view-only) mode.
 * Admin (full edit) mode is unlocked with a password.
 * Default password: "admin123" - change it from the admin menu.
 */
class AuthService {

    private val prefs: Preferences = Preferences.userNodeForPackage(AuthService::class.java)
    private var adminMode = false

    fun isAdmin(): Boolean {
        return adminMode
    }

    fun verify(password: String?): Boolean {
        val want = prefs.get(HASH_KEY, hash(DEFAULT_PASSWORD))
        val ok = MessageDigest.isEqual(bytes(hash(password ?: "")), bytes(want))
        if (ok) {
            adminMode = true
        }
        return ok
    }

    fun logout() {
        adminMode = false
    }

    fun changePassword(current: String?, next: String?): Boolean {
        val want = prefs.get(HASH_KEY, hash(DEFAULT_PASSWORD))
        if (!MessageDigest.isEqual(bytes(hash(current ?: "")), bytes(want))) {
            return false
        }
        if (next == null || next.length < 4) {
            return false
        }
        prefs.put(HASH_KEY, hash(next))
        return true
    }

    companion object {
        private const val HASH_KEY = "admin_password_hash"
        private const val DEFAULT_PASSWORD = "admin123"

        private fun bytes(s: String): ByteArray {
            return s.toByteArray(StandardCharsets.UTF_8)
        }

        @JvmStatic
        fun hash(password: String): String {
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val d = md.digest(("countdown-app|$password").toByteArray(StandardCharsets.UTF_8))
                val sb = StringBuilder(d.size * 2)
                for (b in d) {
                    sb.append(String.format("%02x", b))
                }
                return sb.toString()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }
    }
}
