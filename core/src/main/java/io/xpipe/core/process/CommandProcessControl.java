package io.xpipe.core.process;

import io.xpipe.core.charsetter.Charsetter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public interface CommandProcessControl extends ProcessControl {

    public CommandProcessControl doesNotObeyReturnValueConvention();

    @Override
    public CommandProcessControl sensitive();

    CommandProcessControl complex();

    CommandProcessControl workingDirectory(String directory);

    ShellProcessControl getParent();

    InputStream startExternalStdout() throws Exception;

    OutputStream startExternalStdin() throws Exception;

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

    public default boolean discardAndCheckExit() throws ProcessOutputException {
        try {
            discardOrThrow();
            return true;
        }  catch (ProcessOutputException ex) {
            if (ex.isTimeOut()) {
                throw ex;
            }

            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    void discardOut();

    void discardErr();
}
