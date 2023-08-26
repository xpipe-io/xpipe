package io.xpipe.core.process;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.store.CommandMessageFormatter;
import io.xpipe.core.util.FailableFunction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public interface CommandControl extends ProcessControl {

    enum TerminalExitMode {
        KEEP_OPEN,
        CLOSE
    }

    CommandMessageFormatter getMessageFormatter();

    CommandControl withExceptionConverter(ExceptionConverter converter);

    CommandControl withMessageFormatter(CommandMessageFormatter formatter);

    CommandControl terminalExitMode(TerminalExitMode mode);

    CommandControl doesNotObeyReturnValueConvention();

    CommandControl complex();

    CommandControl notComplex();

    CommandControl withWorkingDirectory(String directory);

    default void execute() throws Exception {
        try (var c = start()) {
            c.discardOrThrow();
        }
    }

    default boolean executeAndCheck() throws Exception {
        try (var c = start()) {
            return c.discardAndCheckExit();
        }
    }

    ShellControl getParent();

    InputStream startExternalStdout() throws Exception;

    OutputStream startExternalStdin() throws Exception;

    boolean waitFor();

    CommandControl withCustomCharset(Charset charset);

    int getExitCode();

    default CommandControl elevated(String message) {
        return elevated(message, (v) -> true);
    }

    CommandControl elevated(String message, FailableFunction<ShellControl, Boolean, Exception> elevationFunction);

    @Override
    CommandControl start() throws Exception;

    CommandControl exitTimeout(Integer timeout);

    void withStdoutOrThrow(Charsetter.FailableConsumer<InputStreamReader, Exception> c);

    String readStdoutDiscardErr() throws Exception;

    void discardOrThrow() throws Exception;

    void accumulateStdout(Consumer<String> con);

    void accumulateStderr(Consumer<String> con);

    byte[] readRawBytesOrThrow() throws Exception;

    String readStdoutOrThrow() throws Exception;

    default boolean discardAndCheckExit() throws ProcessOutputException {
        try {
            discardOrThrow();
            return true;
        } catch (ProcessOutputException ex) {
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
