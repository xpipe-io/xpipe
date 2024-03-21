package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ProcessOutputException;

import java.util.Optional;

public class AppShellCheck {

    public static void check() throws Exception {
        var err = selfTestErrorCheck();

        var canFallback = OsType.getLocal() != OsType.WINDOWS &&
                !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ProcessControlProvider.get().getFallbackDialect());
        if (err.isPresent() && canFallback) {
            enableFallback();
            err = selfTestErrorCheck();
        }

        if (err.isPresent()) {
            var msg =
                    """
                    Shell self-test failed for %s:
                    %s

                    This indicates that something is seriously wrong and certain shell functionality will not work as expected.

                    The most likely causes are:
                    - On Windows, an AntiVirus program might block required programs and commands
                    - The system shell is restricted or blocked
                    - Some elementary command-line tools are not available or not working correctly

                    You can try to switch to the fallback shell by going to Settings -> Local Shell -> Enable Fallback Shell and restart.
                    """
                            .formatted(
                                    ProcessControlProvider.get()
                                            .getEffectiveLocalDialect()
                                            .getDisplayName(),
                                    err.get());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).handle();
        }
    }

    private static void enableFallback() throws Exception {
        LocalShell.reset();
        ProcessControlProvider.get().toggleFallbackShell();
        LocalShell.init();
    }

    private static Optional<String> selfTestErrorCheck() {
        try (var command = LocalShell.getShell().command("echo test").complex().start()) {
            var out = command.readStdoutOrThrow();
            if (!out.equals("test")) {
                return Optional.of("Expected \"test\", got \"" + out + "\"");
            }
        } catch (ProcessOutputException ex) {
            return Optional.ofNullable(ex.getOutput());
        } catch (Throwable t) {
            return Optional.ofNullable(t.getMessage());
        }
        return Optional.empty();
    }
}
