package io.xpipe.ext.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;

import java.util.List;
import java.util.regex.Pattern;

public enum CsvHeaderState {
    @JsonProperty("included")
    INCLUDED,
    @JsonProperty("omitted")
    OMITTED;

    public static CsvHeaderState determine(ArrayNode ar, Character quoteChar) {
        if (ar.size() == 1) {
            return INCLUDED;
        }

        for (int i = 0; i < ar.at(0).size(); i++) {
            if (!matchesQuoting(ar, i, quoteChar)) {
                return INCLUDED;
            }

            if (!matchesPotentialHeader(ar, i, quoteChar)) {
                return INCLUDED;
            }
        }

        return OMITTED;
    }

    private static boolean matchesQuoting(ArrayNode ar, int col, Character quoteChar) {
        var allContent = ar.getNodes().stream()
                .skip(1)
                .allMatch(t -> isQuoted(t.getNodes().get(col), quoteChar));
        if (allContent) {
            return isQuoted(ar.at(0).at(col), quoteChar);
        }

        var noneContent = ar.getNodes().stream()
                .skip(1)
                .noneMatch(t -> isQuoted(t.getNodes().get(col), quoteChar));
        if (noneContent) {
            return !isQuoted(ar.at(0).at(col), quoteChar);
        }

        return true;
    }

    private static boolean isQuoted(DataStructureNode n, Character quoteChar) {
        return !n.asString().equals(strip(n.asString(), quoteChar));
    }

    private static boolean matchesPotentialHeader(ArrayNode ar, int col, Character quoteChar) {
        var t = getForColumnData(ar, col, quoteChar);
        var headerType = getForColumnHeader(ar, col, quoteChar);
        return t.equals(headerType);
    }

    private static String strip(String s, Character quoteChar) {
        s = s.trim();
        if (quoteChar == null) {
            return s;
        }

        if (s.startsWith(quoteChar.toString()) && s.endsWith(quoteChar.toString()) && s.length() > 1) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static GeneralType getForColumnHeader(ArrayNode ar, int col, Character quoteChar) {
        for (var type : GeneralType.TYPES) {
            if (!type.matches(strip(ar.at(0).at(col).asString(), quoteChar))) {
                continue;
            }

            return type;
        }

        throw new IllegalStateException();
    }

    private static GeneralType getForColumnData(ArrayNode ar, int col, Character quoteChar) {
        out:
        for (var type : GeneralType.TYPES) {
            for (int i = 1; i < ar.size(); i++) {
                if (!type.matches(strip(ar.at(i).at(col).asString(), quoteChar))) {
                    continue out;
                }
            }

            return type;
        }

        throw new IllegalStateException();
    }

    private static interface GeneralType {

        static List<GeneralType> TYPES = List.of(new NumberType(), new TextType());

        boolean matches(String s);
    }

    private static class NumberType implements GeneralType {

        private static final Pattern PATTERN = Pattern.compile("^-?\\d*(\\.\\d+)?$");

        @Override
        public boolean matches(String s) {
            return PATTERN.matcher(s).matches();
        }
    }

    private static class TextType implements GeneralType {

        @Override
        public boolean matches(String s) {
            return true;
        }
    }
}
