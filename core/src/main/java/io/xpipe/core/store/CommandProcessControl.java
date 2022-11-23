package io.xpipe.core.store;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public interface CommandProcessControl extends ProcessControl {

    default InputStream startExternalStdout() throws Exception {
        try {
            start();

            AtomicReference<String> err = new AtomicReference<>("");
            accumulateStderr(s -> err.set(s));

            return new FilterInputStream(getStdout()) {
                @Override
                public void close() throws IOException {
                    CommandProcessControl.this.close();
                    if (!err.get().isEmpty()) {
                        throw new IOException(err.get());
                    }
                }
            };
        } catch (Exception ex) {
            close();
            throw ex;
        }
    }

    default OutputStream startExternalStdin() throws Exception {
        try {
            start();
            discardOut();
            discardErr();
            return new FilterOutputStream(getStdin()) {
                @Override
                public void close() throws IOException {
                    closeStdin();
                    CommandProcessControl.this.close();
                }
            };
        } catch (Exception ex) {
            close();
            throw ex;
        }
    }

    public boolean waitFor();

    CommandProcessControl customCharset(Charset charset);

    int getExitCode();

    CommandProcessControl elevated();

    @Override
    CommandProcessControl start() throws Exception;

    @Override
    CommandProcessControl exitTimeout(Integer timeout);

    String readOnlyStdout() throws Exception;

    public default void discardOrThrow() throws Exception {
        readOrThrow();
    }

    void accumulateStdout(Consumer<String> con);

    void accumulateStderr(Consumer<String> con);

    public String readOrThrow() throws Exception;

    public default boolean startAndCheckExit() {
        try (var pc = start()) {
            return pc.discardAndCheckExit();
        } catch (Exception e) {
            return false;
        }
    }

    public default boolean discardAndCheckExit() {
        try {
            discardOrThrow();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    void discardOut();

    void discardErr();
}
