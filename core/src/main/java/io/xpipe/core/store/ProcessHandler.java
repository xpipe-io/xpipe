package io.xpipe.core.store;

import java.io.OutputStream;

public interface ProcessHandler {

    void handle(OutputStream out, OutputStream err);
}
