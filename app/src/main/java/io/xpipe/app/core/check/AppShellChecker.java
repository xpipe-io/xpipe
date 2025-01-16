package io.xpipe.app.core.check;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ProcessOutputException;

import lombok.Value;

import java.util.Optional;

public abstract class AppShellChecker {

    public void check() throws Exception {
        var err = selfTestErrorCheck();

        var canFallback = !ProcessControlProvider.get()
                        .getEffectiveLocalDialect()
                        .equals(ProcessControlProvider.get().getFallbackDialect())
                && shouldAttemptFallback();
        if (err.isPresent() && canFallback) {
            var msg = formatMessage(err.get().getMessage());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).expected().handle();
            toggleFallback();
            var fallbackErr = selfTestErrorCheck();
            if (fallbackErr.isPresent()) {
                // Toggle back if both shells have issues
                toggleFallback();
            }
            err = fallbackErr;
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

    protected boolean shouldAttemptFallback() {
        return true;
    }

    protected String modifyOutput(String output) {
        return output;
    }

    private String formatMessage(String output) {
        var fallback = !ProcessControlProvider.get()
                        .getEffectiveLocalDialect()
                        .equals(ProcessControlProvider.get().getFallbackDialect())
                ? "XPipe will now attempt to fall back to another shell."
                : "";
        return """
                Shell self-test failed for %s:
                %s

                This indicates that something is seriously wrong and certain shell functionality will not work as expected. Some features like the terminal launcher have to create shell scripts for your external terminal emulator to launch.

                The most likely causes are:
                %s

                %s
                """
                .formatted(
                        ProcessControlProvider.get().getEffectiveLocalDialect().getDisplayName(),
                        modifyOutput(output),
                        listReasons(),
                        fallback);
    }

    protected abstract String listReasons();

    private void toggleFallback() throws Exception {
        LocalShell.reset();
        ProcessControlProvider.get().toggleFallbackShell();
        LocalShell.init();
    }

    private Optional<FailureResult> selfTestErrorCheck() {
        try (var sc = LocalShell.getShell().start()) {
            var scriptFile = ScriptHelper.getExecScriptFile(sc);
            var scriptContent = sc.getShellDialect().prepareScriptContent("echo test");
            sc.view().writeScriptFile(scriptFile, scriptContent);
            var out = sc.command(sc.getShellDialect().runScriptCommand(sc, scriptFile.toString()))
                    .readStdoutOrThrow();
            if (!out.equals("test")) {
                return Optional.of(new FailureResult(
                        "Expected output \"test\", got output \"" + out + "\" when running test script", true));
            }
        } catch (ProcessOutputException ex) {
            return Optional.of(new FailureResult(ex.getOutput() != null ? ex.getOutput() : ex.getMessage(), true));
        } catch (Throwable t) {
            return Optional.of(new FailureResult(t.getMessage() != null ? t.getMessage() : t.toString(), false));
        }
        return Optional.empty();
    }

    @Value
    public static class FailureResult {

        String message;
        boolean canContinue;
    }
}
