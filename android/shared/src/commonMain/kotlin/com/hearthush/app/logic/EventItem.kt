package com.hearthush.app.logic

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
    var message: String = ""
    var secretMessage: String = ""
    var secretEnabled: Boolean = false
    var featured: Boolean = false
    var repeatYearly: Boolean = true
    var soundEnabled: Boolean = true
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

    fun nextOccurrence(today: LocalDate): LocalDate {
        if (!repeatYearly) return date
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

    fun isDueToday(today: LocalDate): Boolean {
        return if (repeatYearly) {
            date.month == today.month && date.dayOfMonth == today.dayOfMonth
        } else {
            date == today
        }
    }

    fun isPast(today: LocalDate): Boolean {
        if (repeatYearly) return false
        return date.isBefore(today)
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
        if (isDueToday(now.toLocalDate())) return "00d 00:00:00 • HERE"
        val next = nextOccurrence(now.toLocalDate()).atStartOfDay()
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

    fun dateLabel(): String {
        val f = DateTimeFormatter.ofPattern("dd MMMM")
        return if (repeatYearly) date.format(f) + " • yearly"
        else date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    }

    fun toJson(): String {
        return "{\"id\":" + q(id) +
            ",\"title\":" + q(title) +
            ",\"date\":" + q(date.toString()) +
            ",\"message\":" + q(message) +
            ",\"secretMessage\":" + q(secretMessage) +
            ",\"secretEnabled\":" + secretEnabled +
            ",\"featured\":" + featured +
            ",\"repeatYearly\":" + repeatYearly +
            ",\"soundEnabled\":" + soundEnabled +
            ",\"icon\":" + q(icon) +
            ",\"accentHex\":" + q(accentHex) +
            ",\"category\":" + q(category) +
            ",\"senderId\":" + q(senderId) +
            ",\"forPartner\":" + forPartner +
            ",\"delivered\":" + delivered +
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
                        "message" -> e.message = JsonUtil.unquote(`val`)
                        "secretMessage" -> e.secretMessage = JsonUtil.unquote(`val`)
                        "secretEnabled" -> e.secretEnabled = `val`.toBoolean()
                        "featured" -> e.featured = `val`.toBoolean()
                        "repeatYearly" -> e.repeatYearly = `val`.toBoolean()
                        "soundEnabled" -> e.soundEnabled = `val`.toBoolean()
                        "icon" -> e.icon = JsonUtil.unquote(`val`)
                        "accentHex" -> e.accentHex = JsonUtil.unquote(`val`)
                        "color" -> e.accentHex = JsonUtil.unquote(`val`)
                        "category" -> e.category = JsonUtil.unquote(`val`)
                        "kind" -> e.category = JsonUtil.unquote(`val`)
                        "senderId" -> e.senderId = JsonUtil.unquote(`val`)
                        "forPartner" -> e.forPartner = `val`.toBoolean()
                        "delivered" -> e.delivered = `val`.toBoolean()
                        "createdAt" -> e.createdAt = LocalDateTime.parse(JsonUtil.unquote(`val`))
                        else -> {}
                    }
                } catch (ignored: Exception) {
                }
            }
            if (e.icon.isEmpty()) e.icon = "📅"
            if (e.category.isEmpty()) e.category = "Countdown"
            return e
        }

        private fun pad2(v: Long): String = if (v < 10) "0$v" else "$v"

        private fun q(s: String?): String {
            return "\"" + JsonUtil.escape(s ?: "") + "\""
        }
    }
}
