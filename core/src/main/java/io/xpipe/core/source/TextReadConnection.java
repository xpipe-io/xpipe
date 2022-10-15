package io.xpipe.core.source;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TextReadConnection extends DataSourceReadConnection {

    Stream<String> lines() throws Exception;

    default String readAll() throws Exception {
        try (Stream<String> lines = lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    default void forward(DataSourceConnection con) throws Exception {
        var tCon = (TextWriteConnection) con;
        for (var it = lines().iterator(); it.hasNext(); ) {
            tCon.writeLine(it.next());
        }
    }
}
