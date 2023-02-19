package io.xpipe.ext.csv;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.NamedCharacter;
import lombok.Value;

import java.util.Comparator;
import java.util.List;

@Value
public class CsvDelimiter {

    public static CsvDelimiter getDefault() {
        return ALL.get(0);
    }

    public static final List<CsvDelimiter> ALL = List.of(
            new CsvDelimiter(new NamedCharacter(',', List.of("comma"), "csv.comma"), false),
            new CsvDelimiter(new NamedCharacter(';', List.of("semicolon"), "csv.semicolon"), false),
            new CsvDelimiter(new NamedCharacter(':', List.of("colon"), "csv.colon"), false),
            new CsvDelimiter(new NamedCharacter('|', List.of("pipe"), "csv.pipe"), false),
            new CsvDelimiter(new NamedCharacter('=', List.of("equals"), "csv.equals"), false),
            new CsvDelimiter(new NamedCharacter(' ', List.of("space"), "csv.space"), true),
            new CsvDelimiter(new NamedCharacter('\t', List.of("tab"), "csv.tab"), true));

    NamedCharacter namedCharacter;
    boolean allowMultiple;

    public static boolean allowsMultiple(Character character) {
        return ALL.stream()
                .filter(csvDelimiter -> csvDelimiter.getNamedCharacter().getCharacter() == character)
                .map(CsvDelimiter::isAllowMultiple)
                .findFirst()
                .orElse(false);
    }

    private static int getUnquotedOccurenceCount(String line, char quote, char character) {
        int counter = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == quote) {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes && line.charAt(i) == character) {
                counter++;
            }
        }
        return counter;
    }

    private static boolean isPossibleDelimiter(List<String> lines, char quote, char character) {
        var c = getUnquotedOccurenceCount(lines.get(0), quote, character);
        return lines.stream().allMatch(s -> getUnquotedOccurenceCount(s, quote, character) == c);
    }

    public static CsvDelimiter detectDelimiter(List<String> lines, Character quote) {
        return CsvDelimiter.ALL.stream()
                .filter(c ->
                        isPossibleDelimiter(lines, quote, c.getNamedCharacter().getCharacter()))
                .max(Comparator.comparingInt(c -> getUnquotedOccurenceCount(
                        lines.get(0), quote, c.getNamedCharacter().getCharacter())))
                .orElse(ALL.get(0));
    }

    public static boolean hasMultipleColumns(List<String> lines, Character quote) {
        var usedQuote = quote != null ? quote : '"';
        var delim = detectDelimiter(lines, usedQuote);
        var count = getUnquotedOccurenceCount(
                lines.get(0), usedQuote, delim.getNamedCharacter().getCharacter());

        TrackEvent.withTrace("Multiple column detection finished")
                .tag("delimiter", delim)
                .tag("occurenceCount", count)
                .handle();

        return count > 0;
    }
}
