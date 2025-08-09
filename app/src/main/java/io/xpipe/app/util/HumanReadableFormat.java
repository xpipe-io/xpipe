package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class HumanReadableFormat {

    public static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d LLL yyyy");
    public static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d LLL");
    public static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    public static String byteCount(long bytes) {
        var b = 1024;
        if (-b < bytes && bytes < b) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        var mb = b * b;
        while (bytes <= -mb || bytes >= mb) {
            bytes /= b;
            ci.next();
        }
        var f = "%.1f";
        var r = String.format(f + " %cB", bytes / (double) b, ci.current());
        if (r.endsWith(".0")) {
            r = r.substring(0, r.length() - 2);
        }
        return r;
    }

    public static String progressByteCount(long bytes) {
        var b = 1024;
        if (-b < bytes && bytes < b) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        var mb = b * b;
        while (bytes <= -mb || bytes >= mb) {
            bytes /= b;
            ci.next();
        }

        var f = ci.getIndex() >= 2 ? "%.3f" : "%.1f";
        var r = String.format(f + " %cB", bytes / (double) b, ci.current());
        if (r.endsWith(".0")) {
            r = r.substring(0, r.length() - 2);
        }
        return r;
    }

    public static String date(LocalDateTime x) {
        Objects.requireNonNull(x);
        var now = LocalDateTime.now(ZoneId.systemDefault());

        // not this year
        if (x.getYear() != now.getYear()) {
            return DAY_MONTH_YEAR
                    .withLocale(AppI18n.activeLanguage().getValue().getLocale())
                    .format(x);
        }

        var time = HOUR_MINUTE
                .withLocale(AppI18n.activeLanguage().getValue().getLocale())
                .format(x);
        var date = DAY_MONTH
                .withLocale(AppI18n.activeLanguage().getValue().getLocale())
                .format(x);
        return date + " " + time;
    }

    public static String transferSpeed(long bps) {
        var s = progressByteCount(bps);
        return s + "/s";
    }

    public static String duration(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .replaceAll("\\.\\d+", "")
                .toLowerCase();
    }
}
