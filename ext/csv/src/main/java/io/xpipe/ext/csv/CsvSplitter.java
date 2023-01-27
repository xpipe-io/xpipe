package io.xpipe.ext.csv;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.util.ArrayList;
import java.util.List;

public class CsvSplitter {

    public static List<String> splitCleanedWithDelimiter(String line, Character quote, Character delimiter) {
        var split = splitWithDelimiter(line, quote, delimiter);
        return split.stream().map(s -> CsvQuoteChar.strip(s.trim(), quote)).toList();
    }

    public static List<String> splitWithDelimiter(String line, Character quote, Character delimiter) {
        List<String> split = new ArrayList<>();
        int lastSplit = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            if (quote != null && line.charAt(i) == quote) {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes && delimiter != null && line.charAt(i) == delimiter) {
                var entry = line.substring(lastSplit, i);
                lastSplit = i + 1;
                if (entry.length() == 0) {
                    if (CsvDelimiter.allowsMultiple(delimiter)) {
                        continue;
                    }
                    split.add("");
                } else {
                    split.add(entry);
                }
            }
        }
        split.add(line.substring(lastSplit));
        return split;
    }

    public static ArrayNode splitRaw(List<String> lines, Character quote, Character delimiter) {
        List<DataStructureNode> n = new ArrayList<>();
        for (String line : lines) {
            var s = splitWithDelimiter(line, quote, delimiter).stream()
                    .map(String::trim)
                    .<DataStructureNode>map(ValueNode::of)
                    .toList();
            var tuple = TupleNode.of(s);
            n.add(tuple);
        }
        return ArrayNode.of(n);
    }
}
