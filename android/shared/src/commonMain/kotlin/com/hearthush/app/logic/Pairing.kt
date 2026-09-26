package com.hearthush.app.logic

import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Online pairing + delivery over plain HTTPS (ntfy.sh topics, no accounts).
 *
 * Two devices pair by exchanging 6-letter codes (told to each other
 * out-of-band). After both sides enter each other's code the devices are
 * linked. The link can only be severed when BOTH sides agree.
 */
object PairNet {
    /** Overridable for tests. Production uses the public ntfy.sh service. */
    var BASE = "https://ntfy.sh"

    fun inboxTopic(code: String): String = "hearthush-in-${code.lowercase()}"

    fun pairTopic(a: String, b: String): String {
        val x = a.lowercase()
        val y = b.lowercase()
        return if (x < y) "hearthush-p-$x-$y" else "hearthush-p-$y-$x"
    }
}

data class IncomingReq(val code: String, val accountId: String, val at: Long)

data class SyncResult(
    var justPaired: Boolean = false,
    var severAsked: Boolean = false,
    var severDeclined: Boolean = false,
    var severed: Boolean = false,
    var changed: Boolean = false,
    var offline: Boolean = false
)

class PairStore(ns: String = "") {
    private val p: String = if (ns.isEmpty()) "" else ns + "_"
    internal fun k(name: String): String = p + name

    val accountId: String
    val myCode: String

    init {
        var id = prefsGet(k("acct_id"))
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString().replace("-", "")
            prefsPut(k("acct_id"), id)
        }
        accountId = id
        var code = prefsGet(k("my_code"))
        if (code.isNullOrEmpty()) {
            code = newCode()
            prefsPut(k("my_code"), code)
        }
        myCode = code
    }

    fun isPaired(): Boolean = prefsGet(k("partner_id"))?.isNotEmpty() == true

    fun partnerId(): String = prefsGet(k("partner_id")) ?: ""
    fun partnerCode(): String = prefsGet(k("partner_code")) ?: ""
    fun pairedAt(): Long = prefsGet(k("paired_at"))?.toLongOrNull() ?: 0L

    fun pendingCode(): String = prefsGet(k("pending_code")) ?: ""
    fun setPending(code: String) = prefsPut(k("pending_code"), code)

    fun wantSever(): Boolean = prefsGet(k("want_sever")) == "1"
    fun setWantSever(v: Boolean) = prefsPut(k("want_sever"), if (v) "1" else "")

    fun pairTopic(): String? {
        if (!isPaired()) return null
        return PairNet.pairTopic(myCode, partnerCode())
    }

    fun completePairing(partnerId: String, partnerCode: String) {
        prefsPut(k("partner_id"), partnerId)
        prefsPut(k("partner_code"), partnerCode)
        prefsPut(k("paired_at"), nowSec().toString())
        setPending("")
        removeIncoming(partnerCode)
        setWantSever(false)
    }

    fun sever(store: EventStore) {
        val exPartner = partnerId()
        prefsPut(k("partner_id"), "")
        prefsPut(k("partner_code"), "")
        prefsPut(k("paired_at"), "")
        setPending("")
        setWantSever(false)
        prefsPut(k("incoming_reqs"), "[]")
        // remove everything shared with the ex-partner; keep personal items
        synchronized(store) {
            val kill = store.items().filter { e ->
                e.forPartner || (e.senderId.isNotEmpty() && e.senderId != accountId && e.senderId == exPartner)
            }.map { it.id }
            for (id in kill) store.delete(id)
        }
    }

    fun incoming(): List<IncomingReq> {
        val raw = prefsGet(k("incoming_reqs")) ?: return emptyList()
        val out = mutableListOf<IncomingReq>()
        try {
            val t = raw.trim()
            if (!t.startsWith("[")) return out
            for (part in JsonUtil.splitTopLevel(t.substring(1, t.length - 1))) {
                val p = part.trim()
                if (!p.startsWith("{")) continue
                val map = flatMap(p)
                val code = map["code"] ?: continue
                out.add(IncomingReq(code, map["id"] ?: "", map["at"]?.toLongOrNull() ?: 0L))
            }
        } catch (ignored: Exception) {
        }
        return out
    }

    fun addIncoming(code: String, accountId: String) {
        val clean = code.trim().uppercase()
        if (clean.isEmpty() || clean == myCode) return
        if (isPaired() && clean == partnerCode()) return
        val cur = incoming().toMutableList()
        if (cur.any { it.code == clean }) return
        cur.add(IncomingReq(clean, accountId, nowSec()))
        saveIncoming(cur)
    }

    fun removeIncoming(code: String) {
        val cur = incoming().filter { it.code != code.trim().uppercase() }
        saveIncoming(cur)
    }

    private fun saveIncoming(list: List<IncomingReq>) {
        val sb = StringBuilder("[")
        for ((i, r) in list.withIndex()) {
            if (i > 0) sb.append(",")
            sb.append("{\"code\":").append(q(r.code))
                .append(",\"id\":").append(q(r.accountId))
                .append(",\"at\":").append(r.at).append("}")
        }
        sb.append("]")
        prefsPut(k("incoming_reqs"), sb.toString())
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        fun newCode(): String {
            val sb = StringBuilder()
            repeat(6) { sb.append(ALPHABET[Random.nextInt(ALPHABET.length)]) }
            return sb.toString()
        }

        fun looksLikeCode(s: String): Boolean {
            val t = s.trim().uppercase()
            if (t.length != 6) return false
            return t.all { ALPHABET.contains(it) }
        }

        internal fun flatMap(obj: String): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (part in JsonUtil.splitTopLevel(obj.trim())) {
                val colon = part.indexOf(':')
                if (colon < 0) continue
                val key = JsonUtil.unquote(part.substring(0, colon).trim())
                val v = part.substring(colon + 1).trim()
                map[key] = if (v.startsWith("\"")) JsonUtil.unquote(v) else v
            }
            return map
        }

        private fun q(s: String): String = "\"" + JsonUtil.escape(s) + "\""
    }
}

class SyncEngine(private val store: EventStore, private val pair: PairStore) {

    /** Poll inbox (+ pair topic when linked) and process everything. Never throws. */
    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        val res = SyncResult()
        try {
            pollTopic(PairNet.inboxTopic(pair.myCode), res)
            pair.pairTopic()?.let { pollTopic(it, res) }
            sendUnsent()
        } catch (e: Exception) {
            res.offline = true
        }
        res
    }

    /** Retry publishing partner countdowns that failed to send while offline. */
    private fun sendUnsent() {
        if (!pair.isPaired()) return
        val topic = pair.pairTopic() ?: return
        for (e in store.items()) {
            if (!e.forPartner || !e.isMine(pair.accountId)) continue
            if (prefsGet(pair.k("sent_" + e.id)) != null) continue
            try {
                httpPost(PairNet.BASE + "/" + topic, envelope("countdown", e.toJson()), 12000)
                prefsPut(pair.k("sent_" + e.id), "1")
            } catch (ignored: Exception) {
            }
        }
    }

    fun markSent(id: String) = prefsPut(pair.k("sent_" + id), "1")

    suspend fun sendPairRequest(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = code.trim().uppercase()
            val data = "{\"code\":" + q(pair.myCode) + ",\"id\":" + q(pair.accountId) + "}"
            httpPost(PairNet.BASE + "/" + PairNet.inboxTopic(clean), envelope("pair-request", data), 12000)
            pair.setPending(clean)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendCountdown(item: EventItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val topic = pair.pairTopic() ?: return@withContext false
            httpPost(PairNet.BASE + "/" + topic, envelope("countdown", item.toJson()), 12000)
            markSent(item.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendDelivered(itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val topic = pair.pairTopic() ?: return@withContext false
            httpPost(PairNet.BASE + "/" + topic, envelope("delivered", "{\"id\":" + q(itemId) + "}"), 12000)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** I want out: tell the partner, wait for their agreement (or instant if asked already). */
    suspend fun requestSever(): Boolean = withContext(Dispatchers.IO) {
        if (!pair.isPaired()) return@withContext false
        pair.setWantSever(true)
        return@withContext try {
            val topic = pair.pairTopic() ?: return@withContext true
            httpPost(PairNet.BASE + "/" + topic, envelope("unpair-request", "{}"), 12000)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Partner asked out and I agree: tell them it's done, then sever locally. */
    suspend fun agreeSever(): Boolean = withContext(Dispatchers.IO) {
        if (!pair.isPaired()) return@withContext false
        val topic = pair.pairTopic()
        if (topic != null) {
            try {
                httpPost(PairNet.BASE + "/" + topic, envelope("unpair-done", "{}"), 12000)
            } catch (ignored: Exception) {
            }
        }
        pair.sever(store)
        return@withContext true
    }

    suspend fun declineSever(): Boolean = withContext(Dispatchers.IO) {
        if (!pair.isPaired()) return@withContext false
        return@withContext try {
            val topic = pair.pairTopic() ?: return@withContext false
            httpPost(PairNet.BASE + "/" + topic, envelope("unpair-decline", "{}"), 12000)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------- internals
    private fun pollTopic(topic: String, res: SyncResult) {
        val sinceKey = pair.k("ntfy_since_") + topic
        val since = prefsGet(sinceKey) ?: ""
        // First poll replays the topic cache so nothing sent earlier is missed.
        val url = if (since.isEmpty()) PairNet.BASE + "/" + topic + "/json?since=all"
        else PairNet.BASE + "/" + topic + "/json?since=" + since
        val body = try {
            httpGet(url, 9000)
        } catch (e: Exception) {
            res.offline = true
            return
        }
        var maxId: String? = null
        for (line in body.lines()) {
            val t = line.trim()
            if (!t.startsWith("{")) continue
            val top = PairStore.flatMap(t)
            if (top["event"] != "message") continue
            val nid = top["id"]
            if (nid != null) maxId = nid
            val raw = extractRaw(t, "message") ?: continue
            handleEnvelope(raw, res)
        }
        if (maxId != null) prefsPut(sinceKey, maxId)
    }

    private fun handleEnvelope(raw: String, res: SyncResult) {
        val env = try {
            PairStore.flatMap(raw)
        } catch (e: Exception) {
            return
        }
        val type = env["type"] ?: return
        val from = env["from"] ?: ""
        if (from.isEmpty() || from == pair.accountId) return
        val dataRaw = extractRaw(raw, "data") ?: "{}"
        when (type) {
            "pair-request" -> {
                val d = PairStore.flatMap(dataRaw)
                val code = (d["code"] ?: "").uppercase()
                if (code.isEmpty()) return
                if (pair.isPaired() && code == pair.partnerCode() && from == pair.partnerId()) {
                    // repair: re-confirm an existing link
                    publishQuiet(pair.pairTopic()!!, envelope("pair-accept", "{\"code\":" + q(pair.myCode) + "}"))
                    return
                }
                if (pair.isPaired()) return
                pair.addIncoming(code, from)
                // mutual: I already entered their code -> we are linked
                if (pair.pendingCode() == code) {
                    pair.completePairing(from, code)
                    publishQuiet(
                        PairNet.pairTopic(pair.myCode, code),
                        envelope("pair-accept", "{\"code\":" + q(pair.myCode) + "}")
                    )
                    res.justPaired = true
                }
                res.changed = true
            }
            "pair-accept" -> {
                val d = PairStore.flatMap(dataRaw)
                val code = (d["code"] ?: "").uppercase()
                if (!pair.isPaired() && pair.pendingCode() == code && code.isNotEmpty()) {
                    pair.completePairing(from, code)
                    res.justPaired = true
                    res.changed = true
                }
            }
            "countdown" -> {
                if (!pair.isPaired() || from != pair.partnerId()) return
                try {
                    val item = EventItem.fromJson(dataRaw)
                    item.senderId = from
                    store.addOrUpdate(item)
                    res.changed = true
                } catch (ignored: Exception) {
                }
            }
            "delivered" -> {
                val d = PairStore.flatMap(dataRaw)
                val id = d["id"] ?: return
                val item = store.byId(id) ?: return
                if (item.isMine(pair.accountId) && !item.delivered) {
                    item.delivered = true
                    store.addOrUpdate(item)
                    res.changed = true
                }
            }
            "unpair-request" -> {
                if (!pair.isPaired() || from != pair.partnerId()) return
                if (pair.wantSever()) {
                    // both agreed (I asked, they asked) -> sever now
                    pair.sever(store)
                    res.severed = true
                    res.changed = true
                } else {
                    res.severAsked = true
                }
            }
            "unpair-decline" -> {
                if (!pair.isPaired() || from != pair.partnerId()) return
                pair.setWantSever(false)
                res.severDeclined = true
            }
            "unpair-done" -> {
                // partner severed after agreement; if I also agreed, drop locally
                if (pair.isPaired() && from == pair.partnerId() && pair.wantSever()) {
                    pair.sever(store)
                    res.severed = true
                    res.changed = true
                }
            }
        }
    }

    private fun publishQuiet(topic: String, body: String) {
        try {
            httpPost(PairNet.BASE + "/" + topic, body, 10000)
        } catch (ignored: Exception) {
        }
    }

    private fun envelope(type: String, data: String): String {
        val id = UUID.randomUUID().toString().replace("-", "")
        return "{\"v\":1,\"type\":" + q(type) +
            ",\"from\":" + q(pair.accountId) +
            ",\"id\":" + q(id) +
            ",\"at\":" + nowSec() +
            ",\"data\":" + q(data) + "}"
    }

    private fun q(s: String): String = "\"" + JsonUtil.escape(s) + "\""

    /** Extract a JSON string value (unescaped) for key, or null. */
    internal fun extractRaw(obj: String, key: String): String? {
        val needle = "\"$key\":"
        var i = obj.indexOf(needle)
        if (i < 0) return null
        i += needle.length
        while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
        if (i >= obj.length || obj[i] != '"') return null
        i++
        val sb = StringBuilder()
        var esc = false
        while (i < obj.length) {
            val c = obj[i]
            if (esc) {
                sb.append('\\')
                sb.append(c)
                esc = false
            } else if (c == '\\') esc = true
            else if (c == '"') break
            else sb.append(c)
            i++
        }
        return JsonUtil.unquote("\"$sb\"")
    }
}
