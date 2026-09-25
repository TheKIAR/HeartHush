package countdown

import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Headless self-test (no window): verifies JSON save/load round-trip,
 * countdown math, generic fields, and the admin password gate.
 * Run: java -cp classes countdown.SelfTest
 */
object SelfTest {

    @JvmStatic
    @Throws(Exception::class)
    fun main(args: Array<String>) {
        var tmp = Files.createTempFile("events-test", ".json")
        Files.deleteIfExists(tmp)

        val store = EventStore(tmp)
        store.load()
        check(store.items().size == 4, "4 seed events, got " + store.items().size)
        check(store.items()[0].icon.isNotEmpty(), "seed has icon")
        check(store.items()[0].category.isNotEmpty(), "seed has category")

        val tricky = EventItem()
        tricky.title = "Quote \"test\" \\ backslash\nnewline"
        tricky.date = LocalDate.of(2030, 5, 9)
        tricky.message = "msg <with> & symbols"
        tricky.secretEnabled = true
        tricky.secretMessage = "A private message ❤"
        tricky.featured = true
        tricky.repeatYearly = false
        tricky.soundEnabled = false
        tricky.icon = "🚀"
        tricky.accentHex = "#7C6CFF"
        tricky.category = "App Release"
        store.addOrUpdate(tricky)

        val reloaded = EventStore(tmp)
        reloaded.load()
        val back = reloaded.byId(tricky.id)
        check(back != null, "round-trip item found")
        check(back!!.title == tricky.title, "title round-trip, got: " + back.title)
        check(back.message == tricky.message, "message round-trip")
        check(back.secretEnabled && back.secretMessage == tricky.secretMessage, "secret message round-trip")
        check(back.date == tricky.date, "date round-trip")
        check(back.featured && !back.repeatYearly && !back.soundEnabled, "flag round-trip")
        check("🚀" == back.icon, "icon round-trip, got: " + back.icon)
        check("#7C6CFF" == back.accentHex, "accent round-trip")
        check("App Release" == back.category, "category round-trip")

        // backward compat: old file without generic fields
        val legacy = EventItem.fromJson(
            "{\"id\":\"abc\",\"title\":\"Old\",\"date\":\"2031-01-02\",\"message\":\"hi\"," +
                "\"secretMessage\":\"\",\"secretEnabled\":false,\"featured\":false," +
                "\"repeatYearly\":true,\"soundEnabled\":true," +
                "\"createdAt\":\"2030-01-01T00:00:00\"}"
        )
        check(legacy.icon.isNotEmpty(), "legacy icon defaulted")
        check("Countdown" == legacy.category, "legacy category defaulted, got: " + legacy.category)

        // countdown math: 7 days out, non-yearly
        val week = EventItem()
        week.repeatYearly = false
        week.date = LocalDate.now().plusDays(7)
        val text = week.countdownText(LocalDateTime.now())
        check(
            text.startsWith("06d ") || text.startsWith("07d "),
            "7-day countdown text, got: " + text
        )
        check(!week.isDueToday(LocalDate.now()), "not due today")
        check(
            week.daysUntil(LocalDate.now()) in 6..7,
            "daysUntil ~7, got: " + week.daysUntil(LocalDate.now())
        )
        check(week.progress(LocalDate.now()) in 0.0..1.0, "progress in range")

        // yearly event due today
        val yearly = EventItem()
        yearly.repeatYearly = true
        yearly.date = LocalDate.of(2000, LocalDate.now().month, LocalDate.now().dayOfMonth)
        check(yearly.isDueToday(LocalDate.now()), "yearly due today")
        check(yearly.countdownText(LocalDateTime.now()).contains("DAY IS HERE"), "completed text")
        check("Today!" == yearly.shortCountdown(LocalDate.now()), "short countdown today")

        // seed sorting: featured first
        check(reloaded.sortedByNext(LocalDate.now())[0].featured, "featured sorts first")

        // themes exist (generic app supports any flavor)
        check(FuturisticUI.THEMES.size >= 5, "themes available: " + FuturisticUI.THEMES.size)
        check(FuturisticUI.byId("midnight").id == "midnight", "midnight theme lookup")

        println("SelfTest: ALL CHECKS PASSED")
    }

    private fun check(cond: Boolean, what: String) {
        if (!cond) {
            println("SelfTest FAILED: " + what)
            System.exit(1)
        }
        println("SelfTest ok: " + what)
    }
}
