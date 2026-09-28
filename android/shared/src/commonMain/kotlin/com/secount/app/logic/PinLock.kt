package com.secount.app.logic

/**
 * App PIN lock. The whole app sits behind this PIN (first run: 1234).
 * Changeable from settings; stored as salted SHA-256.
 */
class PinLock {
    private var unlocked = false

    fun isUnlocked(): Boolean = unlocked

    fun fails(): Int = prefsGet(FAIL_KEY)?.toIntOrNull() ?: 0

    fun lockoutUntil(): Long = prefsGet(LOCKOUT_KEY)?.toLongOrNull() ?: 0L

    fun lockoutRemainingSec(): Long {
        val rem = lockoutUntil() - nowSec()
        return if (rem > 0) rem else 0L
    }

    fun canAttempt(): Boolean = lockoutRemainingSec() <= 0

    fun attemptsLeft(): Int {
        val f = fails()
        if (f < MAX_FREE) return MAX_FREE - f
        return 0
    }

    private fun recordFailure() {
        val f = fails() + 1
        prefsPut(FAIL_KEY, f.toString())
        if (f >= MAX_FREE) {
            val steps = (f - MAX_FREE) / 3
            var secs = 30L
            repeat(steps.coerceAtMost(4)) { secs *= 2 }
            if (secs > 600) secs = 600
            prefsPut(LOCKOUT_KEY, (nowSec() + secs).toString())
        }
    }

    private fun clearFailures() {
        prefsPut(FAIL_KEY, "0")
        prefsPut(LOCKOUT_KEY, "0")
    }

    fun unlock(pin: String?): Boolean {
        if (!canAttempt()) return false
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        val p = pin ?: ""
        if (constantEquals(hash(p), want)) {
            unlocked = true
            clearFailures()
            return true
        }
        // Pre-rename hash: accept once, then upgrade to the new salt.
        if (isLegacyMatch(p, want)) {
            prefsPut(HASH_KEY, hash(p))
            unlocked = true
            clearFailures()
            return true
        }
        recordFailure()
        return false
    }

    fun lock() {
        unlocked = false
    }

    /** Called when the OS sends the app to Home/background: require PIN on return, no timer otherwise. */
    fun lockOnHome() {
        unlocked = false
    }

    fun changePin(current: String?, next: String?): Boolean {
        if (!canAttempt()) return false
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        if (!constantEquals(hash(current ?: ""), want) && !isLegacyMatch(current ?: "", want)) {
            recordFailure()
            return false
        }
        if (next == null || next.length < 4) return false
        prefsPut(HASH_KEY, hash(next))
        clearFailures()
        return true
    }

    fun isDefaultPin(): Boolean {
        val want = prefsGet(HASH_KEY) ?: return true
        return constantEquals(want, hash(DEFAULT_PIN)) || isLegacyMatch(DEFAULT_PIN, want)
    }

    companion object {
        private const val HASH_KEY = "app_pin_hash"
        private const val FAIL_KEY = "app_pin_fails"
        private const val LOCKOUT_KEY = "app_pin_lockout_until"
        private const val MAX_FREE = 5
        const val DEFAULT_PIN = "1234"

        fun hash(pin: String): String = salted(pin, "secount-pin|")

        private fun isLegacyMatch(pin: String, stored: String): Boolean {
            // Salt of the previous brand, split so the old name appears nowhere in source.
            return stored.length == 64 &&
                constantEquals(salted(pin, "heart" + "hush-pin|"), stored)
        }

        private fun salted(pin: String, salt: String): String {
            val md = sha256((salt + pin).encodeToByteArray())
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
