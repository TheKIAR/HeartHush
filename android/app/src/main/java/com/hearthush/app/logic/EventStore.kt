package com.hearthush.app.logic

import java.io.File
import java.time.LocalDate

/**
 * Loads/saves the countdown list as JSON. Same schema as the desktop app,
 * so an events.json file can be copied between the two.
 */
class EventStore(private val file: File) {
    private val backingItems = mutableListOf<EventItem>()

    @Synchronized
    fun items(): MutableList<EventItem> = backingItems

    @Synchronized
    fun load() {
        backingItems.clear()
        try {
            if (file.exists()) {
                val json = file.readText(Charsets.UTF_8).trim()
                if (json.startsWith("[") && json.endsWith("]")) {
                    for (part in JsonUtil.splitTopLevel(json.substring(1, json.length - 1))) {
                        val t = part.trim()
                        if (t.startsWith("{")) backingItems.add(EventItem.fromJson(t))
                    }
                }
            }
        } catch (e: Exception) {
            backingItems.clear()
        }
        var changed = false
        if (backingItems.isEmpty()) {
            backingItems.add(
                seed(
                    "Valentine's Day", LocalDate.of(2027, 2, 14),
                    "A little countdown for a very special day.", true,
                    "❤", "#FF5D97", "Holiday"
                )
            )
            backingItems.add(
                seed(
                    "Birthday Bash", LocalDate.now().plusDays(21),
                    "Cake, friends and gifts - don't forget the candles!", false,
                    "🎂", "#FFB020", "Birthday"
                )
            )
            backingItems.add(
                seed(
                    "App Launch 🚀", LocalDate.now().plusDays(45),
                    "Countdown to your Android app / game release.", false,
                    "🚀", "#7C6CFF", "App Release"
                )
            )
            backingItems.add(
                seed(
                    "Final Exams", LocalDate.now().plusDays(12),
                    "Study a little every day - you've got this!", false,
                    "🎓", "#22C4A8", "Exam"
                )
            )
            changed = true
        }
        var hasValentine = false
        for (e in backingItems) {
            if (e.title.lowercase().contains("valentine")) {
                hasValentine = true
                break
            }
        }
        if (!hasValentine) {
            backingItems.add(
                0,
                seed(
                    "Valentine's Day", LocalDate.of(2027, 2, 14),
                    "A little countdown for a very special day.", true,
                    "❤", "#FF5D97", "Holiday"
                )
            )
            changed = true
        }
        for (e in backingItems) {
            if (e.icon.trim().isEmpty()) {
                e.icon = guessIcon(e)
                changed = true
            }
            if (e.category.trim().isEmpty()) {
                e.category = "Countdown"
                changed = true
            }
        }
        if (changed) save()
    }

    @Synchronized
    fun save() {
        try {
            file.parentFile?.mkdirs()
            val sb = StringBuilder("[\n")
            for (i in backingItems.indices) {
                sb.append("  ").append(backingItems[i].toJson())
                if (i + 1 < backingItems.size) sb.append(",")
                sb.append("\n")
            }
            sb.append("]\n")
            file.writeText(sb.toString(), Charsets.UTF_8)
        } catch (ignored: Exception) {
        }
    }

    @Synchronized
    fun addOrUpdate(item: EventItem) {
        for (e in backingItems) {
            if (e.id == item.id) {
                e.title = item.title
                e.date = item.date
                e.message = item.message
                e.secretMessage = item.secretMessage
                e.secretEnabled = item.secretEnabled
                e.featured = item.featured
                e.repeatYearly = item.repeatYearly
                e.soundEnabled = item.soundEnabled
                e.icon = item.icon
                e.accentHex = item.accentHex
                e.category = item.category
                save()
                return
            }
        }
        backingItems.add(item)
        save()
    }

    @Synchronized
    fun delete(id: String) {
        backingItems.removeAll { it.id == id }
        save()
    }

    @Synchronized
    fun byId(id: String): EventItem? {
        for (e in backingItems) if (e.id == id) return e
        return null
    }

    @Synchronized
    fun sortedByNext(today: LocalDate): List<EventItem> {
        val copy = ArrayList(backingItems)
        copy.sortWith(compareBy<EventItem> { !it.featured }.thenBy { it.nextOccurrence(today) })
        return copy
    }

    companion object {
        private fun guessIcon(e: EventItem): String {
            val t = e.title.lowercase() + " " + e.displayCategory().lowercase()
            if (t.contains("birthday") || t.contains("cake")) return "🎂"
            if (t.contains("exam") || t.contains("school") || t.contains("graduation")) return "🎓"
            if (t.contains("wedding") || t.contains("anniversary")) return "💖"
            if (t.contains("trip") || t.contains("travel") || t.contains("flight")) return "✈"
            if (t.contains("app") || t.contains("release") || t.contains("launch") || t.contains("android")) return "🚀"
            if (t.contains("game")) return "🎮"
            if (t.contains("music") || t.contains("concert")) return "🎵"
            if (t.contains("sport") || t.contains("match") || t.contains("football")) return "⚽"
            if (t.contains("work") || t.contains("meeting")) return "💻"
            return "❤"
        }

        private fun seed(
            title: String,
            date: LocalDate,
            message: String,
            featured: Boolean,
            icon: String,
            accentHex: String,
            category: String
        ): EventItem {
            val e = EventItem()
            e.title = title
            e.date = date
            e.message = message
            e.featured = featured
            e.repeatYearly = true
            e.soundEnabled = true
            e.icon = icon
            e.accentHex = accentHex
            e.category = category
            return e
        }
    }
}
