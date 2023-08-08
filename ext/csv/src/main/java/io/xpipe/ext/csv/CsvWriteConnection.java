package io.xpipe.ext.csv;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.impl.SimpleTableWriteConnection;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.TableMapping;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvWriteConnection extends StreamWriteConnection implements SimpleTableWriteConnection<CsvSource> {

    @Getter
    private final CsvSource source;

    private boolean writtenType;

    private List<String> rawColumnNames;
    private List<CsvQuoteState> quoteStates;

    public CsvWriteConnection(CsvSource source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    public void detect() throws Exception {
        if (!store.canOpen()) {
            return;
        }

        List<String> lines = new ArrayList<>(100);
        var result = Charsetter.get().detect(store);
        try (var reader = new BufferedReader(Charsetter.get().reader(store, result.getCharset()))) {
            for (int i = 0; i < 100; i++) {
                var line = reader.readLine();
                if (line == null) {
                    break;
                }

                lines.add(line.trim());
            }
        }

        // Remove invalid lines!
        lines.removeIf(String::isEmpty);

        if (lines.size() == 0) {
            return;
        }

        var array = CsvSplitter.splitRaw(lines, source.getQuote(), source.getDelimiter());
        var headerState = CsvHeaderState.determine(array, source.getQuote());
        var quoteStates = CsvQuoteState.determine(array, headerState, source.getQuote());

        this.rawColumnNames = headerState == CsvHeaderState.INCLUDED
                ? array.at(0).getNodes().stream()
                        .map(DataStructureNode::asString)
                        .toList()
                : null;
        this.quoteStates = quoteStates;
    }

    @Override
    public void init() throws Exception {
        detect();

        super.init();
        if (rawColumnNames != null) {
            var size = rawColumnNames.size();
            for (int i = 0; i < size; i++) {
                writer.write(rawColumnNames.get(i));
                if (i < size - 1) {
                    writer.write(source.getDelimiter());
                }
            }
            writer.write(source.getNewLine().getNewLineString());
            writtenType = true;
        }
    }

    private void writeHeaderType(TupleNode t) throws IOException {
        if (source.getHeaderState() != null && source.getHeaderState().equals(CsvHeaderState.OMITTED)) {
            return;
        }

        if (writtenType) {
            return;
        }

        if (t.getKeyNames().stream().allMatch(Objects::isNull)) {
            writtenType = true;
            return;
        }

        var size = t.size();
        for (int i = 0; i < size; i++) {
            var quoteState = quoteStates != null ? quoteStates.get(i) : null;
            var containsDelimiter =
                    t.getKeyNames().get(i).contains(source.getDelimiter().toString());
            var quoted = quoteState == CsvQuoteState.ALWAYS || containsDelimiter;
            if (quoted) {
                var s = source.getQuote().toString()
                        + t.getKeyNames().get(i)
                        + source.getQuote().toString();
                writer.write(s);
            } else {
                var s = t.getKeyNames().get(i);
                writer.write(s);
            }

            if (i < size - 1) {
                writer.write(source.getDelimiter());
            }
        }
        writer.write(source.getNewLine().getNewLineString());

        writtenType = true;
    }

    private void write(DataStructureNode n) throws IOException {
        if (n.size() == 0) {
            return;
        }

        writeHeaderType(n.asTuple());
        writeLine(n.asTuple());
    }

    private void writeLine(TupleNode t) throws IOException {
        for (int i = 0; i < t.size(); i++) {
            if (t.at(i).hasMetaAttribute(DataStructureNode.IS_NULL)) {
                if (i < t.size() - 1) {
                    writer.write(source.getDelimiter());
                }
                continue;
            }
            var quoteState = quoteStates != null ? quoteStates.get(i) : null;
            var containsDelimiter =
                    t.at(i).asString().contains(source.getDelimiter().toString());
            var quoted = containsDelimiter
                    || quoteState == CsvQuoteState.ALWAYS
                    || t.at(i).hasMetaAttribute(DataStructureNode.IS_TEXT);
            if (quoted) {
                var s = source.getQuote().toString()
                        + t.at(i).asString()
                        + source.getQuote().toString();
                writer.write(s);
            } else {
                writer.write(t.at(i).asString());
            }

            if (i < t.size() - 1) {
                writer.write(source.getDelimiter());
            }
        }
        writer.write(source.getNewLine().getNewLineString());
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        return t -> {
            write(t);
            return true;
        };
    }
}
