package com.secount.app.logic

import androidx.compose.ui.graphics.Color
import java.time.DateTimeException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Countdown model. Same JSON schema everywhere, so events.json files are
 * interchangeable between Android and desktop.
 */
class EventItem {
    var id: String = UUID.randomUUID().toString().replace("-", "")
    var title: String = ""
    var date: LocalDate = LocalDate.now().plusDays(7)
    var hour: Int = 9
    var minute: Int = 0
    var message: String = ""
    var secretMessage: String = ""
    var secretEnabled: Boolean = false
    var featured: Boolean = false
    var repeatYearly: Boolean = true
    /** once | yearly | monthly | weekly. Empty = legacy (use repeatYearly). */
    var repeatMode: String = ""
    var soundEnabled: Boolean = true
    /** Chime | Soft | Silent */
    var soundName: String = "Chime"
    var remind1d: Boolean = true
    var remind7d: Boolean = false
    /** Full reply conversation, lines of "epochSec|who|text". replyMessage keeps latest for compat. */
    var replyThread: String = ""
    /** Attached photo filename (app photos dir). Empty = none. */
    var photoUri: String = ""
    var createdAt: LocalDateTime = LocalDateTime.now()

    /** Emoji / symbol shown on the card. */
    var icon: String = "❤"

    /** Accent color as #RRGGBB. Empty = use app brand color. */
    var accentHex: String = ""

    /** Free-form category: Birthday, Exam, Wedding, App Release, Holiday, Work, ... */
    var category: String = "Countdown"

    /** Account id of the creator. Empty = created before pairing existed. */
    var senderId: String = ""

    /** True = addressed to the partner (they reveal it at zero). */
    var forPartner: Boolean = false

    /** True once the partner has opened it at zero (receipt). */
    var delivered: Boolean = false

    /** Reply thread on a shared secret (receiver answers the sender). */
    var replyMessage: String = ""

    fun effectiveRepeat(): String {
        if (repeatMode == "once" || repeatMode == "yearly" || repeatMode == "monthly" || repeatMode == "weekly") return repeatMode
        return if (repeatYearly) "yearly" else "once"
    }

    fun setRepeat(mode: String) {
        repeatMode = mode
        repeatYearly = mode != "once"
    }

    fun nextOccurrence(today: LocalDate): LocalDate {
        when (effectiveRepeat()) {
            "once" -> return date
            "weekly" -> {
                if (!today.isAfter(date)) return date
                var d = date
                var guard = 0
                while (d.isBefore(today) && guard < 520) {
                    d = d.plusWeeks(1)
                    guard++
                }
                return d
            }
            "monthly" -> {
                if (!today.isAfter(date)) return date
                var y = today.year
                var m = today.monthValue
                repeat(25) {
                    val len = LocalDate.of(y, m, 1).lengthOfMonth()
                    val cand = LocalDate.of(y, m, minOf(date.dayOfMonth, len))
                    if (!cand.isBefore(today) && !cand.isBefore(date)) return cand
                    m++
                    if (m > 12) {
                        m = 1
                        y++
                    }
                }
                return date
            }
            else -> {
                val day = minOf(
                    date.dayOfMonth,
                    LocalDate.of(today.year, date.month, 1).lengthOfMonth()
                )
                val candidate: LocalDate = try {
                    LocalDate.of(today.year, date.month, day)
                } catch (e: DateTimeException) {
                    LocalDate.of(today.year, date.month, 1)
                        .withDayOfMonth(LocalDate.of(today.year, date.month, 1).lengthOfMonth())
                }
                return if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
            }
        }
    }

    fun isDueToday(today: LocalDate): Boolean {
        if (today.isBefore(date)) return false
        return when (effectiveRepeat()) {
            "once" -> date == today
            "weekly" -> date.dayOfWeek == today.dayOfWeek
            "monthly" -> {
                val want = minOf(date.dayOfMonth, today.lengthOfMonth())
                today.dayOfMonth == want
            }
            else -> date.month == today.month && date.dayOfMonth == today.dayOfMonth
        }
    }

    fun isPast(today: LocalDate): Boolean {
        if (effectiveRepeat() != "once") return false
        return date.isBefore(today)
    }

    fun targetDateTime(today: LocalDate): LocalDateTime {
        val d = if (effectiveRepeat() == "once") date else nextOccurrence(today)
        return try {
            d.atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        } catch (e: Exception) {
            d.atStartOfDay()
        }
    }

    fun daysUntil(today: LocalDate): Long {
        val next = if (repeatYearly) nextOccurrence(today) else date
        return ChronoUnit.DAYS.between(today, next)
    }

    /** 0..1 progress from creation toward the target (for progress bars). */
    fun progress01(today: LocalDate): Float {
        try {
            val start = createdAt.toLocalDate()
            val end = if (repeatYearly) nextOccurrence(today) else date
            val total = ChronoUnit.DAYS.between(start, end)
            if (total <= 0) return if (isDueToday(today)) 1f else 0f
            val done = ChronoUnit.DAYS.between(start, today)
            var p = done.toFloat() / total.toFloat()
            if (p < 0f) p = 0f
            if (p > 1f) p = 1f
            return p
        } catch (e: Exception) {
            return 0f
        }
    }

    fun hasSecret(): Boolean {
        return secretEnabled && secretMessage.trim().isNotEmpty()
    }

    fun displayIcon(): String {
        return if (icon.trim().isEmpty()) "📅" else icon
    }

    fun displayCategory(): String {
        return if (category.trim().isEmpty()) "Countdown" else category
    }

    /** True if this device created the countdown. */
    fun isMine(myId: String): Boolean = senderId.isEmpty() || senderId == myId

    /** True if the partner sent it to me (I reveal it at zero). */
    fun isForMe(myId: String): Boolean = forPartner && senderId.isNotEmpty() && senderId != myId

    fun accentColor(fallback: Color): Color {
        return parseAccent(accentHex) ?: fallback
    }

    fun countdownText(now: LocalDateTime): String {
        val today = now.toLocalDate()
        if (isDueToday(today)) {
            val target = targetDateTime(today)
            if (!now.isBefore(target)) return "00d 00:00:00 • HERE"
            var s = Duration.between(now, target).seconds
            if (s < 0) s = 0
            return "00d ${pad2(s / 3600)}:${pad2((s % 3600) / 60)}:${pad2(s % 60)} • TODAY"
        }
        val next = targetDateTime(today)
        var s = Duration.between(now, next).seconds
        if (s < 0) s = 0
        return "${pad2(s / 86400)}d ${pad2((s % 86400) / 3600)}:${pad2((s % 3600) / 60)}:${pad2(s % 60)}"
    }

    fun shortCountdown(today: LocalDate): String {
        if (isDueToday(today)) return "Today!"
        val d = daysUntil(today)
        if (d < 0) return "${-d}d ago"
        if (d == 1L) return "Tomorrow"
        return "$d days left"
    }

    fun timeLabel(): String = "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun repeatLabel(): String = when (effectiveRepeat()) {
        "yearly" -> "yearly"
        "monthly" -> "monthly"
        "weekly" -> "weekly"
        else -> "one-time"
    }

    fun dateLabel(): String {
        val f = DateTimeFormatter.ofPattern("dd MMMM")
        val full = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        val base = if (effectiveRepeat() == "once") date.format(full) else date.format(f)
        return "$base • ${timeLabel()} • ${repeatLabel()}"
    }

    fun threadEntries(): List<Triple<Long, String, String>> {
        val out = mutableListOf<Triple<Long, String, String>>()
        if (replyThread.isBlank()) {
            if (replyMessage.isNotBlank()) out.add(Triple(0L, "partner", replyMessage))
            return out
        }
        for (line in replyThread.lines()) {
            val t = line.trim()
            if (t.isEmpty()) continue
            val p1 = t.indexOf('|')
            if (p1 < 0) {
                out.add(Triple(0L, "", t))
                continue
            }
            val p2 = t.indexOf('|', p1 + 1)
            if (p2 < 0) {
                out.add(Triple(0L, "", t))
                continue
            }
            out.add(Triple(t.substring(0, p1).toLongOrNull() ?: 0L, t.substring(p1 + 1, p2), t.substring(p2 + 1)))
        }
        if (out.isEmpty() && replyMessage.isNotBlank()) out.add(Triple(0L, "partner", replyMessage))
        return out
    }

    fun appendReply(who: String, text: String, atSec: Long) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val safe = clean.replace("\n", " ")
        val entry = "$atSec|${who.replace("|", "")}|$safe"
        replyThread = if (replyThread.isBlank()) entry else replyThread + "\n" + entry
        replyMessage = clean
    }

    fun toJson(): String {
        return "{\"id\":" + q(id) +
            ",\"title\":" + q(title) +
            ",\"date\":" + q(date.toString()) +
            ",\"hour\":" + hour.coerceIn(0, 23) +
            ",\"minute\":" + minute.coerceIn(0, 59) +
            ",\"message\":" + q(message) +
            ",\"secretMessage\":" + q(secretMessage) +
            ",\"secretEnabled\":" + secretEnabled +
            ",\"featured\":" + featured +
            ",\"repeatYearly\":" + repeatYearly +
            ",\"repeatMode\":" + q(effectiveRepeat()) +
            ",\"soundEnabled\":" + soundEnabled +
            ",\"soundName\":" + q(soundName) +
            ",\"remind1d\":" + remind1d +
            ",\"remind7d\":" + remind7d +
            ",\"icon\":" + q(icon) +
            ",\"accentHex\":" + q(accentHex) +
            ",\"category\":" + q(category) +
            ",\"senderId\":" + q(senderId) +
            ",\"forPartner\":" + forPartner +
            ",\"delivered\":" + delivered +
            ",\"replyMessage\":" + q(replyMessage) +
            ",\"replyThread\":" + q(replyThread) +
            ",\"photoUri\":" + q(photoUri) +
            ",\"createdAt\":" + q(createdAt.toString()) + "}"
    }

    fun copyFromJson(): EventItem = fromJson(toJson())

    companion object {
        val CATEGORY_PRESETS = listOf(
            "Countdown", "Birthday", "Anniversary", "Wedding", "Holiday",
            "Exam", "Graduation", "App Release", "Game Launch", "Trip",
            "Work", "Health", "Custom"
        )

        val ICON_PRESETS = listOf(
            "❤", "🎂", "🎉", "⭐", "🚀",
            "🎓", "✈", "💖", "🏠", "🎵",
            "⚽", "💻", "🎮", "🌟", "⏰", "🔔"
        )

        fun parseAccent(hex: String?): Color? {
            try {
                if (hex == null) return null
                var h = hex.trim()
                if (h.isEmpty()) return null
                if (h.startsWith("#")) h = h.substring(1)
                if (h.length == 6) {
                    val r = h.substring(0, 2).toInt(16)
                    val g = h.substring(2, 4).toInt(16)
                    val b = h.substring(4, 6).toInt(16)
                    return Color(r, g, b)
                }
            } catch (ignored: Exception) {
            }
            return null
        }

        fun fromJson(obj: String): EventItem {
            val e = EventItem()
            for (part in JsonUtil.splitTopLevel(obj.trim())) {
                val colon = part.indexOf(':')
                if (colon < 0) continue
                val key = JsonUtil.unquote(part.substring(0, colon).trim())
                val `val` = part.substring(colon + 1).trim()
                try {
                    when (key) {
                        "id" -> e.id = JsonUtil.unquote(`val`)
                        "title" -> e.title = JsonUtil.unquote(`val`)
                        "date" -> e.date = LocalDate.parse(JsonUtil.unquote(`val`))
                        "hour" -> e.hour = JsonUtil.unquote(`val`).toIntOrNull() ?: (`val`.toIntOrNull() ?: 9)
                        "minute" -> e.minute = JsonUtil.unquote(`val`).toIntOrNull() ?: (`val`.toIntOrNull() ?: 0)
                        "message" -> e.message = JsonUtil.unquote(`val`)
                        "secretMessage" -> e.secretMessage = JsonUtil.unquote(`val`)
                        "secretEnabled" -> e.secretEnabled = `val`.toBoolean()
                        "featured" -> e.featured = `val`.toBoolean()
                        "repeatYearly" -> e.repeatYearly = `val`.toBoolean()
                        "repeatMode" -> e.repeatMode = JsonUtil.unquote(`val`)
                        "repeat" -> e.repeatMode = JsonUtil.unquote(`val`)
                        "soundEnabled" -> e.soundEnabled = `val`.toBoolean()
                        "soundName" -> e.soundName = JsonUtil.unquote(`val`)
                        "remind1d" -> e.remind1d = `val`.toBoolean()
                        "remind7d" -> e.remind7d = `val`.toBoolean()
                        "icon" -> e.icon = JsonUtil.unquote(`val`)
                        "accentHex" -> e.accentHex = JsonUtil.unquote(`val`)
                        "color" -> e.accentHex = JsonUtil.unquote(`val`)
                        "category" -> e.category = JsonUtil.unquote(`val`)
                        "kind" -> e.category = JsonUtil.unquote(`val`)
                        "senderId" -> e.senderId = JsonUtil.unquote(`val`)
                        "forPartner" -> e.forPartner = `val`.toBoolean()
                        "delivered" -> e.delivered = `val`.toBoolean()
                        "replyMessage" -> e.replyMessage = JsonUtil.unquote(`val`)
                        "reply" -> e.replyMessage = JsonUtil.unquote(`val`)
                        "replyThread" -> e.replyThread = JsonUtil.unquote(`val`)
                        "photoUri" -> e.photoUri = JsonUtil.unquote(`val`)
                        "photo" -> e.photoUri = JsonUtil.unquote(`val`)
                        "createdAt" -> e.createdAt = LocalDateTime.parse(JsonUtil.unquote(`val`))
                        else -> {}
                    }
                } catch (ignored: Exception) {
                }
            }
            if (e.icon.isEmpty()) e.icon = "📅"
            if (e.category.isEmpty()) e.category = "Countdown"
            if (e.repeatMode.isEmpty()) e.repeatMode = if (e.repeatYearly) "yearly" else "once"
            e.repeatYearly = e.repeatMode != "once"
            e.hour = e.hour.coerceIn(0, 23)
            e.minute = e.minute.coerceIn(0, 59)
            if (e.soundName.isEmpty()) e.soundName = "Chime"
            if (e.replyThread.isEmpty() && e.replyMessage.isNotEmpty()) {
                e.replyThread = "0|partner|" + e.replyMessage.replace("\n", " ")
            }
            return e
        }

        private fun pad2(v: Long): String = if (v < 10) "0$v" else "$v"

        private fun q(s: String?): String {
            return "\"" + JsonUtil.escape(s ?: "") + "\""
        }
    }
}
