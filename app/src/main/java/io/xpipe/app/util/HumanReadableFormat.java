package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

public final class HumanReadableFormat {

    public static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd LLL yyyy");
    public static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd LLL");
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

    // Same format as docker uses for container states
    public static String age(Duration duration) {
        var seconds = duration.toSeconds();
        if (seconds < 1) {
            return "Less than a second";
        }
        if (seconds == 1) {
            return "1 second";
        }
        if (seconds < 60) {
            return seconds + " seconds";
        }

        var minutes = duration.toMinutes();
        if (minutes == 1) {
            return "About a minute";
        }
        if (minutes < 60) {
            return minutes + " minutes";
        }

        var hours = Math.round(duration.toSeconds() / 3600.0);
        if (hours == 1) {
            return "About an hour";
        }
        if (hours < 48) {
            return hours + " hours";
        }
        if (hours < 24 * 7 * 2) {
            return (hours / 24) + " days";
        }
        if (hours < 24 * 30 * 2) {
            return (hours / 24 / 7) + " weeks";
        }
        if (hours < 24 * 365 * 2) {
            return (hours / 24 / 30) + " months";
        }
        return (duration.toHours() / 24 / 365) + " years";
    }

    public static String duration(Duration duration) {
        var s = duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .replaceAll("\\.\\d+", "")
                .toLowerCase();
        var padded = Pattern.compile("\\d+").matcher(s).replaceAll(matchResult -> {
            var r = matchResult.group();
            if (r.length() == 1) {
                return "0" + r;
            } else {
                return r;
            }
        });
        return padded;
    }
}
