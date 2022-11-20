package io.xpipe.core.store;

import java.io.*;
import java.nio.charset.Charset;

public interface CommandProcessControl extends ProcessControl {

    default InputStream startExternalStdout() throws Exception {
        start();
        discardErr();
        return new FilterInputStream(getStdout()) {
            @Override
            public void close() throws IOException {
                getStdout().close();
                CommandProcessControl.this.close();
            }
        };
    }

    default OutputStream startExternalStdin() throws Exception {
        try (CommandProcessControl pc = start()) {
            pc.discardOut();
            pc.discardErr();
            return new FilterOutputStream(getStdin()) {
                @Override
                public void close() throws IOException {
                    pc.getStdin().close();
                    pc.close();
                }
            };
        } catch (Exception e) {
            throw e;
        }
    }

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

    Thread discardOut();

    Thread discardErr();
}
