package io.xpipe.app.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Objects;

public final class HumanReadableFormat {

    public static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d LLL yyyy");
    public static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d LLL");
    public static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("EEE");
    public static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    public static String byteCount(long bytes) {
        if (-1024 < bytes && bytes < 1024) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -1024 * 1024 || bytes >= 1024 * 1024) {
            bytes /= 1024;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1024.0, ci.current());
    }

    public static String date(LocalDateTime x) {
        Objects.requireNonNull(x);
        var now = LocalDateTime.now(ZoneId.systemDefault());

        // not this year
        if (x.getYear() != now.getYear()) {
            return DAY_MONTH_YEAR.format(x);
        }

        // not this week
        if (getWeekNumber(x) != getWeekNumber(now)) {
            return DAY_MONTH.format(x);
        }

        // not today
        int xDay = x.getDayOfWeek().getValue();
        int nowDay = now.getDayOfWeek().getValue();
        if (xDay == nowDay - 1) {
            return "Yesterday";
        }
        if (xDay != nowDay) {
            return DAY_OF_WEEK.format(x);
        }

        return HOUR_MINUTE.format(x);
    }

    private static int getWeekNumber(LocalDateTime date) {
        return date.get(WeekFields.of(Locale.getDefault()).weekOfYear());
    }
}
