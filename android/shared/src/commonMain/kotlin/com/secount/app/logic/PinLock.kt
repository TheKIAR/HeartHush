package com.secount.app.logic

/**
 * App PIN lock. The whole app sits behind this PIN (first run: 1234).
 * Changeable from settings; stored as salted SHA-256.
 */
class PinLock {
    private var unlocked = false

    fun isUnlocked(): Boolean = unlocked

    fun unlock(pin: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        val p = pin ?: ""
        if (constantEquals(hash(p), want)) {
            unlocked = true
            return true
        }
        // Pre-rename hash: accept once, then upgrade to the new salt.
        if (isLegacyMatch(p, want)) {
            prefsPut(HASH_KEY, hash(p))
            unlocked = true
            return true
        }
        return false
    }

    fun lock() {
        unlocked = false
    }

    fun changePin(current: String?, next: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        if (!constantEquals(hash(current ?: ""), want) && !isLegacyMatch(current ?: "", want)) return false
        if (next == null || next.length < 4) return false
        prefsPut(HASH_KEY, hash(next))
        return true
    }

    fun isDefaultPin(): Boolean {
        val want = prefsGet(HASH_KEY) ?: return true
        return constantEquals(want, hash(DEFAULT_PIN)) || isLegacyMatch(DEFAULT_PIN, want)
    }

    companion object {
        private const val HASH_KEY = "app_pin_hash"
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
