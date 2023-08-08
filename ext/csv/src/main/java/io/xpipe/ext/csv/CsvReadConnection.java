package io.xpipe.ext.csv;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.impl.StreamReadConnection;
import io.xpipe.core.source.TableReadConnection;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvReadConnection extends StreamReadConnection implements TableReadConnection {

    private final CsvSource source;
    private TupleType dataType;
    private BufferedReader bufferedReader;

    public CsvReadConnection(CsvSource source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public TupleType getDataType() {
        return dataType;
    }

    @Override
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (isEmpty(line)) {
                continue;
            }

            var values = CsvSplitter.splitWithDelimiter(line, source.getQuote(), source.getDelimiter());
            trim(values);

            var names = dataType.getNames();
            var maxLength = Math.max(names.size(), values.size());
            var paddedNames = new ArrayList<>(names);
            for (int i = 0; i < maxLength - names.size(); i++) {
                paddedNames.add(null);
            }
            var paddedValues = new ArrayList<>(values);
            for (int i = 0; i < maxLength - values.size(); i++) {
                paddedValues.add(null);
            }

            var nodes = paddedValues.stream()
                    .map(s -> {
                        if (s == null || s.isEmpty()) {
                            return ValueNode.nullValue();
                        }
                        var quotesRemoved = CsvQuoteChar.strip(s, source.getQuote());
                        return ValueNode.of(quotesRemoved);
                    })
                    .toList();

            if (!lineAcceptor.accept(TupleNode.of(paddedNames, nodes))) {
                break;
            }
        }
    }

    private boolean isEmpty(String line) {
        if (line == null) {
            return true;
        }

        return line.trim().isEmpty();
    }

    @Override
    public void init() throws Exception {
        super.init();
        bufferedReader = new BufferedReader(reader);

        if (source.getHeaderState() == CsvHeaderState.INCLUDED) {
            var line = bufferedReader.readLine();
            if (isEmpty(line)) {
                this.dataType = TupleType.empty();
                return;
            }

            var next = CsvSplitter.splitCleanedWithDelimiter(line, source.getQuote(), source.getDelimiter());
            if (next.size() == 0) {
                this.dataType = TupleType.empty();
                return;
            }

            this.dataType = TupleType.of(next, Collections.nCopies(next.size(), ValueType.of()));
        } else {
            bufferedReader.mark(1000);
            var line = bufferedReader.readLine();
            bufferedReader.reset();

            if (isEmpty(line)) {
                this.dataType = TupleType.empty();
                return;
            }

            var next = CsvSplitter.splitCleanedWithDelimiter(line, source.getQuote(), source.getDelimiter());
            if (next.size() == 0) {
                this.dataType = TupleType.empty();
                return;
            }

            this.dataType = TupleType.of(Collections.nCopies(next.size(), ValueType.of()));
        }
    }

    private List<String> trim(List<String> line) {
        line.replaceAll(String::trim);
        return line;
    }

    @Override
    public void close() throws Exception {
        bufferedReader.close();
        super.close();
    }
}
