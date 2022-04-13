package io.xpipe.core.source;

import java.util.List;
import java.util.stream.Stream;

public interface TextReadConnection extends DataSourceReadConnection {

    /**
     * Reads the complete contents.
     */
    String readAll() throws Exception;

    List<String> readAllLines() throws Exception;

    String readLine() throws Exception;

    Stream<String> lines() throws Exception;

    boolean isFinished() throws Exception;

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (TextWriteConnection) con) {
            tCon.init();
            for (var it = lines().iterator(); it.hasNext(); ) {
                tCon.writeLine(it.next());
            }
        }
    }
}
