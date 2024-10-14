package io.xpipe.app.core.check;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessOutputException;

import lombok.Value;

import java.util.Optional;

public class AppShellCheck {

    public static void check() throws Exception {
        var err = selfTestErrorCheck();

        // We don't want to fall back on macOS as occasional zsh spawn issues would cause many users to use sh
        var canFallback = !ProcessControlProvider.get()
                        .getEffectiveLocalDialect()
                        .equals(ProcessControlProvider.get().getFallbackDialect())
                && OsType.getLocal() != OsType.MACOS;
        if (err.isPresent() && canFallback) {
            var msg = formatMessage(err.get().getMessage());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).expected().handle();
            enableFallback();
            err = selfTestErrorCheck();
        }

        if (err.isPresent()) {
            var msg = formatMessage(err.get().getMessage());
            var event = ErrorEvent.fromThrowable(new IllegalStateException(msg));
            if (!err.get().isCanContinue()) {
                event.term();
            }
            event.handle();
        }
    }

    private static String modifyOutput(String output) {
        if (OsType.getLocal().equals(OsType.WINDOWS)
                && output.contains("is not recognized as an internal or external command")
                && output.contains("exec-")) {
            return "Unable to create temporary script files";
        }

        return output;
    }

    private static String formatMessage(String output) {
        var fallback = !ProcessControlProvider.get()
                        .getEffectiveLocalDialect()
                        .equals(ProcessControlProvider.get().getFallbackDialect())
                ? "XPipe will now attempt to fall back to another shell."
                : "";
        return """
                Shell self-test failed for %s:
                %s

                This indicates that something is seriously wrong and certain shell functionality will not work as expected.

                The most likely causes are:
                - On Windows, an AntiVirus program might block required programs and commands
                - The system shell is restricted or blocked
                - Your PATH environment variable is corrupt / incomplete. You can check this by manually trying to run some commands in a terminal
                - Some elementary command-line tools are not available or not working correctly

                %s
                """
                .formatted(
                        ProcessControlProvider.get().getEffectiveLocalDialect().getDisplayName(),
                        modifyOutput(output),
                        fallback);
    }

    private static void enableFallback() throws Exception {
        LocalShell.reset();
        ProcessControlProvider.get().toggleFallbackShell();
        LocalShell.init();
    }

    private static Optional<FailureResult> selfTestErrorCheck() {
        try (var command = LocalShell.getShell().command("echo test").complex().start()) {
            var out = command.readStdoutOrThrow();
            if (!out.equals("test")) {
                return Optional.of(new FailureResult("Expected \"test\", got \"" + out + "\"", true));
            }
        } catch (ProcessOutputException ex) {
            return Optional.of(new FailureResult(ex.getOutput() != null ? ex.getOutput() : ex.toString(), true));
        } catch (Throwable t) {
            return Optional.of(new FailureResult(t.getMessage() != null ? t.getMessage() : t.toString(), false));
        }
        return Optional.empty();
    }

    @Value
    private static class FailureResult {

        String message;
        boolean canContinue;
    }
}
