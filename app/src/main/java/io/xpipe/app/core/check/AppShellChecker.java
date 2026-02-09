package io.xpipe.app.core.check;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.Value;

import java.util.Optional;

public abstract class AppShellChecker {

    public void check() throws Exception {
        var isDefaultShell = ProcessControlProvider.get()
                .getAvailableLocalDialects()
                .getFirst()
                .equals(ProcessControlProvider.get().getEffectiveLocalDialect());
        if (isDefaultShell && fallBackInstantly()) {
            toggleFallback();
        }

        var originalErr = selfTestErrorCheck();
        if (originalErr.isEmpty()) {
            return;
        }

        // If we are already in fallback mode and can somehow continue, we should do so instantly
        if (originalErr.get().isCanContinue() && !isDefaultShell) {
            return;
        }

        var attemptFallback =
                shouldAttemptFallbackForProcessStartFail() || !originalErr.get().isProcessSpawnIssue();
        if (!attemptFallback) {
            // Sometimes we don't want to fall back
            // The local shell init will fail terminally if it still does not work
            return;
        }

        var msg = formatMessage(originalErr.get().getMessage());
        var fallBack = new SimpleBooleanProperty();
        var newDialect = ProcessControlProvider.get().getNextFallbackDialect();
        var switchAction = createFallbackAction(fallBack, newDialect);
        ErrorEventFactory.fromThrowable(new IllegalStateException(msg))
                .customAction(switchAction)
                .documentationLink(DocumentationLink.LOCAL_SHELL_ERROR)
                .expected()
                .handle();
        if (!fallBack.get() && originalErr.get().isCanContinue()) {
            return;
        }

        toggleFallback();
        var fallbackErr = selfTestErrorCheck();
        if (fallbackErr.isEmpty()) {
            return;
        }

        msg = formatMessage(fallbackErr.get().getMessage());
        var event = ErrorEventFactory.fromThrowable(new IllegalStateException(msg))
                .documentationLink(DocumentationLink.LOCAL_SHELL_ERROR);
        // Only make it terminal if both shells can't continue
        if (!fallbackErr.get().isCanContinue()) {
            event.term();
        }
        event.handle();
    }

    private ErrorAction createFallbackAction(BooleanProperty set, ShellDialect dialect) {
        return new ErrorAction() {
            @Override
            public String getName() {
                return "Fall back to " + dialect.getDisplayName() + " as an alternative shell";
            }

            @Override
            public String getDescription() {
                return "Attempt to handle all operations only using " + dialect.getDisplayName();
            }

            @Override
            public boolean handle(ErrorEvent event) {
                set.set(true);
                return true;
            }
        };
    }

    protected boolean shouldAttemptFallbackForProcessStartFail() {
        return true;
    }

    protected String modifyOutput(String output) {
        return output;
    }

    private String formatMessage(String output) {
        var isDefaultShell = ProcessControlProvider.get()
                .getAvailableLocalDialects()
                .getFirst()
                .equals(ProcessControlProvider.get().getEffectiveLocalDialect());
        var fallback = isDefaultShell
                ? AppNames.ofCurrent().getName() + " will now attempt to fall back to another shell."
                : "";
        return """
               Shell self-test failed for %s:
               %s

               This indicates that something is seriously wrong and certain shell functionality will not work as expected. Some features like the terminal launcher have to create shell scripts for your external terminal emulator to launch.

               %s
               """.formatted(LocalShell.getDialect().getDisplayName(), modifyOutput(output), fallback)
                .strip();
    }

    protected abstract boolean fallBackInstantly();

    private void toggleFallback() throws Exception {
        LocalShell.reset(true);
        ProcessControlProvider.get().toggleFallbackShell();
        LocalShell.init();
    }

    private Optional<FailureResult> selfTestErrorCheck() {
        try (var sc = LocalShell.init()) {
            var scriptContent = "echo test";
            var scriptFile = ScriptHelper.createExecScript(sc, scriptContent);
            var out = sc.command(sc.getShellDialect().runScriptCommand(sc, scriptFile.toString()))
                    .readStdoutOrThrow();
            if (!out.equals("test")) {
                return Optional.of(new FailureResult(
                        "Expected output \"test\", got output \"" + out + "\" when running test script", false, true));
            }
        } catch (ProcessOutputException ex) {
            return Optional.of(
                    new FailureResult(ex.getOutput() != null ? ex.getOutput() : ex.getMessage(), false, true));
        } catch (ShellSpawnException ex) {
            return Optional.of(new FailureResult(ex.getMessage(), true, true));
        } catch (Throwable t) {
            return Optional.of(new FailureResult(t.getMessage() != null ? t.getMessage() : t.toString(), false, false));
        }
        return Optional.empty();
    }

    @Value
    public static class FailureResult {

        String message;
        boolean processSpawnIssue;
        boolean canContinue;
    }
}
