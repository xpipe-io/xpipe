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

        var canFallback = !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ProcessControlProvider.get().getFallbackDialect());
        if (err.isPresent() && canFallback) {
            var msg = formatMessage(err.get());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).handle();
            enableFallback();
            err = selfTestErrorCheck();
        }

        if (err.isPresent()) {
            var msg = formatMessage(err.get());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).handle();
        }
    }

    private static String modifyOutput(String output) {
        if (OsType.getLocal().equals(OsType.WINDOWS) && output.contains("is not recognized as an internal or external command") && output.contains("exec-")) {
            return "Unable to create temporary script files";
        }

        return output;
    }

    private static String formatMessage(String output) {
        var fallback = !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ProcessControlProvider.get().getFallbackDialect()) ? "XPipe will now attempt to fall back to another shell." : "";
        return
                """
                Shell self-test failed for %s:
                %s

                This indicates that something is seriously wrong and certain shell functionality will not work as expected.

                The most likely causes are:
                - On Windows, an AntiVirus program might block required programs and commands
                - The system shell is restricted or blocked
                - Some elementary command-line tools are not available or not working correctly

                %s
                """
                        .formatted(
                                ProcessControlProvider.get()
                                        .getEffectiveLocalDialect()
                                        .getDisplayName(),
                                modifyOutput(output), fallback);
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
            return Optional.of(ex.getOutput() != null ? ex.getOutput() : ex.toString());
        } catch (Throwable t) {
            return Optional.of(t.getMessage() != null ? t.getMessage() : t.toString());
        }
        return Optional.empty();
    }
}
