package countdown

import java.awt.Color
import java.time.DateTimeException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

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

    // Generic-use fields (all optional, backward compatible with old files).
    /** Emoji / symbol shown on the card, e.g. "🎂" "❤" "🚀". */
    var icon: String = "❤"
    /** Accent color as #RRGGBB. Empty = use app theme accent. */
    var accentHex: String = ""
    /** Free-form category: Birthday, Exam, Wedding, App Release, Holiday, Work, ... */
    var category: String = "Countdown"

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
    fun progress(today: LocalDate): Double {
        try {
            val start = createdAt.toLocalDate()
            val end = if (repeatYearly) nextOccurrence(today) else date
            val total = ChronoUnit.DAYS.between(start, end)
            if (total <= 0) return if (isDueToday(today)) 1.0 else 0.0
            val done = ChronoUnit.DAYS.between(start, today)
            var p = done.toDouble() / total.toDouble()
            if (p < 0) p = 0.0
            if (p > 1) p = 1.0
            return p
        } catch (e: Exception) {
            return 0.0
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

    fun accentColor(fallback: Color): Color {
        return parseHex(accentHex) ?: fallback
    }

    fun countdownText(now: LocalDateTime): String {
        if (isDueToday(now.toLocalDate())) return "00d 00:00:00 • DAY IS HERE"
        val next = nextOccurrence(now.toLocalDate()).atStartOfDay()
        var s = Duration.between(now, next).seconds
        if (s < 0) s = 0
        return String.format(
            "%02dd %02d:%02d:%02d",
            s / 86400, (s % 86400) / 3600, (s % 3600) / 60, s % 60
        )
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
            ",\"createdAt\":" + q(createdAt.toString()) + "}"
    }

    companion object {
        val CATEGORY_PRESETS = arrayOf(
            "Countdown", "Birthday", "Anniversary", "Wedding", "Holiday",
            "Exam", "Graduation", "App Release", "Game Launch", "Trip",
            "Work", "Health", "Custom"
        )

        val ICON_PRESETS = arrayOf(
            "❤", "🎂", "🎉", "⭐", "🚀",
            "🎓", "✈", "💖", "🏠", "🎵",
            "⚽", "💻", "🎮", "🌟", "⏰", "🔔"
        )

        @JvmStatic
        fun parseHex(hex: String?): Color? {
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

        @JvmStatic
        fun toHex(c: Color?): String {
            if (c == null) return ""
            return String.format("#%02X%02X%02X", c.red, c.green, c.blue)
        }

        @JvmStatic
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

        private fun q(s: String?): String {
            return "\"" + JsonUtil.escape(s ?: "") + "\""
        }
    }
}
