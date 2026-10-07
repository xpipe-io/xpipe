package io.xpipe.app.util;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

public class DateTimeParseHelper {

    private final DateTimeFormatter formatter;

    public DateTimeParseHelper(String pattern) {
        this(pattern, Locale.US);
    }

    public DateTimeParseHelper(String pattern, Locale locale) {
        this.formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendPattern(pattern)
                .toFormatter(locale);
    }

    public Instant parse(String text) {
        var parsed = formatter.parse(text, new ParsePosition(0));
        var date = parsed.query(TemporalQueries.localDate());
        if (date == null) {
            throw new DateTimeException("No date found in " + text);
        }

        var time = parsed.query(TemporalQueries.localTime());
        return date.atTime(time != null ? time : LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
