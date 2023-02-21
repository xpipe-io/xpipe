package io.xpipe.core.process;

import io.xpipe.core.charsetter.Charsetter;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public interface CommandProcessControl extends ProcessControl {

    public CommandProcessControl doesNotObeyReturnValueConvention();

    @Override
    public CommandProcessControl sensitive();

    CommandProcessControl complex();

    CommandProcessControl workingDirectory(String directory);

    ShellProcessControl getParent();

    default InputStream startExternalStdout() throws Exception {
        start();
        discardErr();
        return new FilterInputStream(getStdout()) {
            @Override
            @SneakyThrows
            public void close() throws IOException {
                CommandProcessControl.this.close();
            }
        };
    }

    default OutputStream startExternalStdin() throws Exception {
        start();
        discardOut();
        discardErr();
        return new FilterOutputStream(getStdin()) {
            @Override
            @SneakyThrows
            public void close() throws IOException {
                closeStdin();
                CommandProcessControl.this.close();
            }
        };
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
