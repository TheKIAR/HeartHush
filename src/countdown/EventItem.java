package countdown;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class EventItem {
    public String id = UUID.randomUUID().toString().replace("-", "");
    public String title = "";
    public LocalDate date = LocalDate.now().plusDays(7);
    public String message = "";
    public String secretMessage = "";
    public boolean secretEnabled = false;
    public boolean featured = false;
    public boolean repeatYearly = true;
    public boolean soundEnabled = true;
    public LocalDateTime createdAt = LocalDateTime.now();

    public LocalDate nextOccurrence(LocalDate today) {
        if (!repeatYearly) return date;
        int day = Math.min(date.getDayOfMonth(),
                LocalDate.of(today.getYear(), date.getMonth(), 1).lengthOfMonth());
        LocalDate candidate;
        try {
            candidate = LocalDate.of(today.getYear(), date.getMonth(), day);
        } catch (DateTimeException e) {
            candidate = LocalDate.of(today.getYear(), date.getMonth(), 1)
                    .withDayOfMonth(LocalDate.of(today.getYear(), date.getMonth(), 1).lengthOfMonth());
        }
        if (candidate.isBefore(today)) candidate = candidate.plusYears(1);
        return candidate;
    }

    public boolean isDueToday(LocalDate today) {
        if (repeatYearly) {
            return date.getMonth() == today.getMonth() && date.getDayOfMonth() == today.getDayOfMonth();
        }
        return date.equals(today);
    }

    public boolean hasSecret() {
        return secretEnabled && secretMessage != null && !secretMessage.trim().isEmpty();
    }

    public String countdownText(LocalDateTime now) {
        if (isDueToday(now.toLocalDate())) return "00d 00:00:00 • COUNTDOWN COMPLETE";
        LocalDateTime next = nextOccurrence(now.toLocalDate()).atStartOfDay();
        long s = java.time.Duration.between(now, next).getSeconds();
        if (s < 0) s = 0;
        return String.format("%02dd %02d:%02d:%02d",
                s / 86400, (s % 86400) / 3600, (s % 3600) / 60, s % 60);
    }

    public String dateLabel() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMMM");
        return repeatYearly ? date.format(f) + " • yearly" :
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }

    public String toJson() {
        return "{\"id\":" + q(id)
                + ",\"title\":" + q(title)
                + ",\"date\":" + q(date.toString())
                + ",\"message\":" + q(message)
                + ",\"secretMessage\":" + q(secretMessage)
                + ",\"secretEnabled\":" + secretEnabled
                + ",\"featured\":" + featured
                + ",\"repeatYearly\":" + repeatYearly
                + ",\"soundEnabled\":" + soundEnabled
                + ",\"createdAt\":" + q(createdAt.toString()) + "}";
    }

    public static EventItem fromJson(String obj) {
        EventItem e = new EventItem();
        for (String part : JsonUtil.splitTopLevel(obj.trim())) {
            int colon = part.indexOf(':');
            if (colon < 0) continue;
            String key = JsonUtil.unquote(part.substring(0, colon).trim());
            String val = part.substring(colon + 1).trim();
            try {
                switch (key) {
                    case "id": e.id = JsonUtil.unquote(val); break;
                    case "title": e.title = JsonUtil.unquote(val); break;
                    case "date": e.date = LocalDate.parse(JsonUtil.unquote(val)); break;
                    case "message": e.message = JsonUtil.unquote(val); break;
                    case "secretMessage": e.secretMessage = JsonUtil.unquote(val); break;
                    case "secretEnabled": e.secretEnabled = Boolean.parseBoolean(val); break;
                    case "featured": e.featured = Boolean.parseBoolean(val); break;
                    case "repeatYearly": e.repeatYearly = Boolean.parseBoolean(val); break;
                    case "soundEnabled": e.soundEnabled = Boolean.parseBoolean(val); break;
                    case "createdAt": e.createdAt = LocalDateTime.parse(JsonUtil.unquote(val)); break;
                    default: break;
                }
            } catch (Exception ignored) {}
        }
        return e;
    }

    private static String q(String s) {
        return "\"" + JsonUtil.escape(s == null ? "" : s) + "\"";
    }
}
