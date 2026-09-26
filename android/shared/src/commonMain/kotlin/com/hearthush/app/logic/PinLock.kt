package com.hearthush.app.logic

/**
 * App PIN lock. The whole app sits behind this PIN (first run: 1234).
 * Changeable from settings; stored as salted SHA-256.
 */
class PinLock {
    private var unlocked = false

    fun isUnlocked(): Boolean = unlocked

    fun unlock(pin: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        val ok = constantEquals(hash(pin ?: ""), want)
        if (ok) unlocked = true
        return ok
    }

    fun lock() {
        unlocked = false
    }

    fun changePin(current: String?, next: String?): Boolean {
        val want = prefsGet(HASH_KEY) ?: hash(DEFAULT_PIN)
        if (!constantEquals(hash(current ?: ""), want)) return false
        if (next == null || next.length < 4) return false
        prefsPut(HASH_KEY, hash(next))
        return true
    }

    fun isDefaultPin(): Boolean {
        val want = prefsGet(HASH_KEY) ?: return true
        return constantEquals(want, hash(DEFAULT_PIN))
    }

    companion object {
        private const val HASH_KEY = "app_pin_hash"
        const val DEFAULT_PIN = "1234"

        fun hash(pin: String): String {
            val md = sha256(("hearthush-pin|$pin").encodeToByteArray())
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
