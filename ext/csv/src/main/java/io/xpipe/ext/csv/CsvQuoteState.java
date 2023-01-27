package io.xpipe.ext.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;

import java.util.ArrayList;
import java.util.List;

public enum CsvQuoteState {
    @JsonProperty("always")
    ALWAYS,
    @JsonProperty("mixed")
    MIXED,
    @JsonProperty("never")
    NEVER;

    public static List<CsvQuoteState> determine(ArrayNode ar, CsvHeaderState headerState, Character quoteChar) {
        var list = new ArrayList<CsvQuoteState>();
        for (int i = 0; i < ar.at(0).size(); i++) {
            list.add(determineForColumn(ar, headerState, i, quoteChar));
        }
        return list;
    }

    public static CsvQuoteState determineForHeader(List<String> rawHeader, Character quoteChar) {
        var allContent = rawHeader.stream().allMatch(t -> isQuoted(t, quoteChar));
        if (allContent) {
            return ALWAYS;
        }

        var noneContent = rawHeader.stream().noneMatch(t -> isQuoted(t, quoteChar));
        if (noneContent) {
            return NEVER;
        }

        return MIXED;
    }

    private static CsvQuoteState determineForColumn(
            ArrayNode ar, CsvHeaderState headerState, int col, Character quoteChar) {
        var skip = headerState == CsvHeaderState.INCLUDED ? 1 : 0;
        var allContent = ar.getNodes().stream()
                .skip(skip)
                .allMatch(t -> isQuoted(t.getNodes().get(col), quoteChar));
        if (allContent) {
            return ALWAYS;
        }

        var noneContent = ar.getNodes().stream()
                .skip(skip)
                .noneMatch(t -> isQuoted(t.getNodes().get(col), quoteChar));
        if (noneContent) {
            return NEVER;
        }

        return MIXED;
    }

    private static boolean isQuoted(String s, Character quoteChar) {
        return !s.equals(CsvQuoteChar.strip(s, quoteChar));
    }

    private static boolean isQuoted(DataStructureNode n, Character quoteChar) {
        return !n.asString().equals(CsvQuoteChar.strip(n.asString(), quoteChar));
    }
}
