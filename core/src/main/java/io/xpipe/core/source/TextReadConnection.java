package io.xpipe.core.source;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public interface TextReadConnection extends DataSourceReadConnection {

    Stream<String> lines() throws Exception;

    boolean isFinished() throws Exception;

    default void forwardLines(OutputStream out, int maxLines) throws Exception {
        if (maxLines == 0) {
            return;
        }

        int counter = 0;
        for (var it = lines().iterator(); it.hasNext(); counter++) {
            if (counter == maxLines) {
                break;
            }

            out.write(it.next().getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (TextWriteConnection) con) {
            tCon.init();
            for (var it = lines().iterator(); it.hasNext(); ) {
                tCon.writeLine(it.next());
            }
        }
    }
}
