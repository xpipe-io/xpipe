package io.xpipe.core.process;

import io.xpipe.core.charsetter.Charsetter;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public interface CommandProcessControl extends ProcessControl {

    public CommandProcessControl doesNotObeyReturnValueConvention();

    @Override
    public CommandProcessControl sensitive();

    CommandProcessControl complex();

    CommandProcessControl workingDirectory(String directory);

    ShellProcessControl getParent();

    default InputStream startExternalStdout() throws Exception {
        try {
            start();

            AtomicReference<String> err = new AtomicReference<>("");
            accumulateStderr(s -> err.set(s));

            return new FilterInputStream(getStdout()) {
                @Override
                @SneakyThrows
                public void close() throws IOException {
                    CommandProcessControl.this.close();
                    if (!err.get().isEmpty()) {
                        throw new IOException(err.get());
                    }
                    CommandProcessControl.this.getParent().restart();
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
                @SneakyThrows
                public void close() throws IOException {
                    closeStdin();
                    CommandProcessControl.this.close();
                    CommandProcessControl.this.getParent().restart();
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

    CommandProcessControl exitTimeout(Integer timeout);

    public void withStdoutOrThrow(Charsetter.FailableConsumer<InputStreamReader, Exception> c) throws Exception;
    String readOnlyStdout() throws Exception;

    public void discardOrThrow() throws Exception;

    void accumulateStdout(Consumer<String> con);

    void accumulateStderr(Consumer<String> con);

    public String readOrThrow() throws Exception;

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
