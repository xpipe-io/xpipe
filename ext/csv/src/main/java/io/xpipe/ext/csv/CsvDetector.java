package io.xpipe.ext.csv;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.extension.event.TrackEvent;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class CsvDetector {

    private static TrackEvent.TrackEventBuilder event(String msg) {
        return TrackEvent.builder().type("trace").category("csv").message(msg);
    }

    public static CsvSource detect(StreamDataStore store, int maxLines) throws Exception {
        event("Starting csv type detection")
                .tag("maxLines", maxLines)
                .tag("store", store.toString())
                .handle();

        if (!store.canOpen()) {
            event("Can't open store").handle();
            return CsvSource.empty(store);
        }

        List<String> lines = new ArrayList<>(maxLines);

        var result = Charsetter.get().detect(store);
        try (var reader = new BufferedReader(Charsetter.get().reader(store, result.getCharset()))) {
            for (int i = 0; i < maxLines; i++) {
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
            event("Amount of lines is 0").handle();
            return CsvSource.empty(store);
        }

        var quote = CsvQuoteChar.detectQuoteChar(lines);
        var quoteChar = quote.getCharacter();
        var hasMultipleColumns = CsvDelimiter.hasMultipleColumns(lines, quoteChar);
        if (!hasMultipleColumns) {
            event("Single column file detected").handle();
            var array = CsvSplitter.splitRaw(lines, quoteChar, null);
            var headerState = CsvHeaderState.determine(array, quoteChar);
            return CsvSource.builder()
                    .store(store)
                    .charset(result.getCharset())
                    .newLine(result.getNewLine())
                    .delimiter(CsvDelimiter.getDefault().getNamedCharacter().getCharacter())
                    .quote(quoteChar)
                    .headerState(headerState)
                    .build();
        }

        var delimiter = CsvDelimiter.detectDelimiter(lines, quoteChar);
        var array =
                CsvSplitter.splitRaw(lines, quoteChar, delimiter.getNamedCharacter().getCharacter());
        var headerState = CsvHeaderState.determine(array, quoteChar);

        event("Finished detection").handle();
        return CsvSource.builder()
                .store(store)
                .charset(result.getCharset())
                .newLine(result.getNewLine())
                .delimiter(delimiter.getNamedCharacter().getCharacter())
                .quote(quoteChar)
                .headerState(headerState)
                .build();
    }
}
