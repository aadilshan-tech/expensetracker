package spendwise;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds an .ics calendar file for loan due dates. Import it into Google Calendar, Outlook or Apple
 * Calendar to be reminded even when SpendWise is not running.
 */
public final class CalendarExport {
    private CalendarExport() {}

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static String build(List<Loan> loans) {
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//SpendWise//Loan Reminders//EN");
        line(sb, "CALSCALE:GREGORIAN");
        String stamp = ZonedDateTime.now(ZoneOffset.UTC).format(STAMP);
        for (Loan l : loans) {
            if (l.isSettled()) continue;
            String summary = l.isBorrowed()
                    ? "Pay back " + Theme.money(l.remaining()) + " to " + l.person
                    : "Collect " + Theme.money(l.remaining()) + " from " + l.person;
            String desc = summary + (l.note.isEmpty() ? "" : "\n" + l.note) + "\nAdded from SpendWise";
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:" + l.id + "@spendwise.local");
            line(sb, "DTSTAMP:" + stamp);
            line(sb, "DTSTART;VALUE=DATE:" + DAY.format(l.dueDate));
            line(sb, "DTEND;VALUE=DATE:" + DAY.format(l.dueDate.plusDays(1)));
            line(sb, "SUMMARY:" + esc(summary));
            line(sb, "DESCRIPTION:" + esc(desc));
            // reminder 1: 9 AM the day before, reminder 2: 9 AM on the day
            alarm(sb, "-PT15H", summary);
            alarm(sb, "PT9H", summary);
            line(sb, "END:VEVENT");
        }
        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static void alarm(StringBuilder sb, String trigger, String text) {
        line(sb, "BEGIN:VALARM");
        line(sb, "ACTION:DISPLAY");
        line(sb, "DESCRIPTION:" + esc(text));
        line(sb, "TRIGGER:" + trigger);
        line(sb, "END:VALARM");
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r", "").replace("\n", "\\n");
    }

    /** Appends one logical line, folded to 75 octets per RFC 5545 and terminated with CRLF. */
    private static void line(StringBuilder out, String s) {
        int octets = 0;
        boolean first = true;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int len = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            int limit = first ? 75 : 74; // continuation lines start with one space
            if (octets + len > limit) {
                out.append("\r\n ");
                octets = 0;
                first = false;
            }
            out.appendCodePoint(cp);
            octets += len;
            i += Character.charCount(cp);
        }
        out.append("\r\n");
    }
}
