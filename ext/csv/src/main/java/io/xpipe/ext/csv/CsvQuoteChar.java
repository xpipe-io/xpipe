package io.xpipe.ext.csv;

import io.xpipe.app.util.NamedCharacter;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class CsvQuoteChar {

    public static final List<NamedCharacter> CHARS = List.of(
            new NamedCharacter('"', List.of("double-quote", "double_quote", "dquote", "quote"), "csv.doubleQuote"),
            new NamedCharacter('\'', List.of("single-quote", "single_quote", "squote"), "csv.singleQuote"));

    public static NamedCharacter detectQuoteChar(List<String> lines) {
        // Check for first char

        for (var c : CHARS) {
            for (var line : lines) {
                if (line.startsWith(String.valueOf(c.getCharacter()))) {
                    return c;
                }
            }
        }

        // Check for even occurrence count
        var possibleCharacters = new ArrayList<>(CHARS);
        out:
        for (var c : CHARS) {
            for (var line : lines) {
                var count = line.chars().filter(ch -> ch == c.getCharacter()).count();
                if (count > 0 && count % 2 != 0) {
                    possibleCharacters.remove(c);
                    continue out;
                }
            }
            return c;
        }

        return possibleCharacters.stream()
                .max(Comparator.comparingInt(c -> lines.stream()
                        .mapToInt(s -> (int)
                                s.chars().filter(ch -> ch == c.getCharacter()).count())
                        .sum()))
                .orElse(CHARS.get(0));
    }

    public static String strip(String s, Character quoteChar) {
        s = s.trim();
        if (quoteChar == null) {
            return s;
        }

        if (s.startsWith(quoteChar.toString()) && s.endsWith(quoteChar.toString()) && s.length() > 1) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }
}
