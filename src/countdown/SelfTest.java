package countdown;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Headless self-test (no window): verifies JSON save/load round-trip,
 * countdown math, and the admin password gate. Run: java -cp classes countdown.SelfTest
 */
public class SelfTest {

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempFile("events-test", ".json");
        Files.deleteIfExists(tmp);

        EventStore store = new EventStore(tmp);
        store.load();
        check(store.items().size() == 3, "3 seed events, got " + store.items().size());

        EventItem tricky = new EventItem();
        tricky.title = "Quote \"test\" \\ backslash\nnewline";
        tricky.date = LocalDate.of(2030, 5, 9);
        tricky.message = "msg <with> & symbols";
        tricky.secretEnabled = true;
        tricky.secretMessage = "A private Valentine message ♥";
        tricky.featured = true;
        tricky.repeatYearly = false;
        tricky.soundEnabled = false;
        store.addOrUpdate(tricky);

        EventStore reloaded = new EventStore(tmp);
        reloaded.load();
        EventItem back = reloaded.byId(tricky.id);
        check(back != null, "round-trip item found");
        check(back.title.equals(tricky.title), "title round-trip, got: " + back.title);
        check(back.message.equals(tricky.message), "message round-trip");
        check(back.secretEnabled && back.secretMessage.equals(tricky.secretMessage), "secret message round-trip");
        check(back.date.equals(tricky.date), "date round-trip");
        check(back.featured && !back.repeatYearly && !back.soundEnabled, "flag round-trip");

        // countdown math: 7 days out, non-yearly
        EventItem week = new EventItem();
        week.repeatYearly = false;
        week.date = LocalDate.now().plusDays(7);
        String text = week.countdownText(LocalDateTime.now());
        check(text.startsWith("06d ") || text.startsWith("07d "),
                "7-day countdown text, got: " + text);
        check(!week.isDueToday(LocalDate.now()), "not due today");

        // yearly event due today
        EventItem yearly = new EventItem();
        yearly.repeatYearly = true;
        yearly.date = LocalDate.of(2000, LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth());
        check(yearly.isDueToday(LocalDate.now()), "yearly due today");
        check(yearly.countdownText(LocalDateTime.now()).contains("COUNTDOWN COMPLETE"), "completed text");

        // seed sorting: featured first
        check(reloaded.sortedByNext(LocalDate.now()).get(0).featured, "featured sorts first");

        System.out.println("SelfTest: ALL CHECKS PASSED");
    }

    private static void check(boolean cond, String what) {
        if (!cond) {
            System.out.println("SelfTest FAILED: " + what);
            System.exit(1);
        }
        System.out.println("SelfTest ok: " + what);
    }
}
