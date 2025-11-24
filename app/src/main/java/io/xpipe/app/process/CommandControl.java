package io.xpipe.app.process;

import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

public interface CommandControl extends ProcessControl {

    // Keep these out of a normal exit code range
    // They have to be in range of 0 - 255 in order to work on all systems
    int UNASSIGNED_EXIT_CODE = 160;
    int EXIT_TIMEOUT_EXIT_CODE = 161;
    int START_FAILED_EXIT_CODE = 162;
    int INTERNAL_ERROR_EXIT_CODE = 163;
    int ELEVATION_FAILED_EXIT_CODE = 164;

    String getDisplayCommand();

    CommandBuilder getTerminalCommand();

    CommandControl sensitive();

    CommandControl withExceptionConverter(ProcessExceptionConverter converter);

    CommandControl start() throws Exception;

    CommandControl withErrorFormatter(Function<String, String> formatter);

    CommandControl notComplex();

    CommandControl withWorkingDirectory(FilePath directory);

    default void execute() throws Exception {
        discardOrThrow();
    }

    default boolean executeAndCheck() throws Exception {
        return discardAndCheckExit();
    }

    ShellControl getParent();

    InputStream startExternalStdout() throws Exception;

    OutputStream startExternalStdin() throws Exception;

    void setExitTimeout(Duration duration);

    void setStartTimeout(Duration duration);

    boolean waitFor();

    CommandControl withCustomCharset(Charset charset);

    long getExitCode();

    CommandControl elevated(ElevationFunction function);

    String[] readStdoutAndStderr() throws Exception;

    void discardOrThrow() throws Exception;

    byte[] readRawBytesOrThrow() throws Exception;

    String readStdoutOrThrow() throws Exception;

    Optional<String> readStdoutIfPossible() throws Exception;

    default void killOnTimeout(CountDown countDown) {
        GlobalTimer.scheduleUntil(Duration.ofSeconds(1), false, () -> {
            if (!isRunning(true)) {
                return false;
            }

            if (!countDown.countDown()) {
                kill();
                return false;
            }

            return true;
        });
    }

    default boolean discardAndCheckExit() throws ProcessOutputException {
        try {
            discardOrThrow();
            return true;
        } catch (ProcessOutputException ex) {
            if (ex.isIrregularExit()) {
                throw ex;
            }

            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
