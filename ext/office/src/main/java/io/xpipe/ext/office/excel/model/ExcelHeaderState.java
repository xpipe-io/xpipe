package io.xpipe.ext.office.excel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.xpipe.core.data.node.ArrayNode;

import java.util.List;
import java.util.regex.Pattern;

public enum ExcelHeaderState {
    @JsonProperty("included")
    INCLUDED,
    @JsonProperty("excluded")
    EXCLUDED;

    public static ExcelHeaderState determine(ArrayNode ar) {
        if (ar.size() == 1) {
            return INCLUDED;
        }

        for (int i = 0; i < ar.at(0).size(); i++) {
            if (!matchesPotentialHeader(ar, i)) {
                return INCLUDED;
            }
        }

        return EXCLUDED;
    }

    private static boolean matchesPotentialHeader(ArrayNode ar, int col) {
        var t = getForColumnData(ar, col);
        var headerType = getForColumnHeader(ar, col);
        return t.equals(headerType);
    }

    private static GeneralType getForColumnHeader(ArrayNode ar, int col) {
        for (var type : GeneralType.TYPES) {
            if (!type.matches(ar.at(0).at(col).asString())) {
                continue;
            }

            return type;
        }

        throw new IllegalStateException();
    }

    private static GeneralType getForColumnData(ArrayNode ar, int col) {
        out:
        for (var type : GeneralType.TYPES) {
            for (int i = 1; i < ar.size(); i++) {
                if (!type.matches(ar.at(i).at(col).asString())) {
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
