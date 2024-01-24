package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellDialects;

import java.util.Optional;

public class AppShellCheck {

    public static void check() {
        var err = selfTestErrorCheck();
        if (err.isPresent()) {
            var msg = """
                    Shell self-test failed for %s:
                    %s
                    
                    This indicates that something is seriously wrong and certain shell functionality will not work as expected.
                    
                    The most likely causes are:
                    - On Windows, an AntiVirus program might block required programs and commands
                    - The system shell is restricted or blocked
                    - The operating system is not supported
                    
                    You can reach out to us if you want to properly diagnose the cause individually and hopefully fix it.
                    """.formatted(ShellDialects.getPlatformDefault().getDisplayName(), err.get());
            ErrorEvent.fromThrowable(new IllegalStateException(msg)).handle();
        }
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
