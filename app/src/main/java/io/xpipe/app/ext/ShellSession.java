package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FailableSupplier;

import lombok.Getter;

@Getter
public class ShellSession extends Session {

    private final FailableSupplier<ShellControl> supplier;
    private ShellControl shellControl;

    public ShellSession(FailableSupplier<ShellControl> supplier) {
        this.supplier = supplier;
    }

    private ShellControl createControl() throws Exception {
        var pc = supplier.get();
        pc.onStartupFail(ignored -> {
            listener.onStateChange(false);
        });
        pc.onInit(ignored -> {
            listener.onStateChange(true);
        });
        pc.onKill(() -> {
            listener.onStateChange(false);
        });
        // Listen for parent exit as onExit is called before exit is completed
        // In case it is stuck, we would not get the right status otherwise
        pc.getParentControl().ifPresent(p -> {
            p.onExit(ignored -> {
                listener.onStateChange(false);
            });
        });
        pc.onExit(ignored -> {
            listener.onStateChange(false);
        });
        return pc;
    }

    public boolean isRunning() {
        if (shellControl == null) {
            return false;
        }

        return shellControl.isRunning(true);
    }

    public void start() throws Exception {
        if (shellControl != null && shellControl.isRunning(true)) {
            return;
        } else {
            stop();
        }

        try {
            shellControl = createControl();
            shellControl.start();

            var shouldAliveCheck = !shellControl.isLocal();
            var supportsAliveCheck =
                    shellControl.getShellDialect().getDumbMode().supportsAnyPossibleInteraction();
            if (shouldAliveCheck && supportsAliveCheck) {
                startAliveListener();
            }
        } catch (Exception ex) {
            try {
                stop();
            } catch (Exception stopEx) {
                ex.addSuppressed(stopEx);
            }
            throw ex;
        }
    }

    public void stop() throws Exception {
        if (shellControl == null) {
            return;
        }

        shellControl.shutdown();
    }

    @Override
    public boolean checkAlive() throws Exception {
        if (shellControl == null) {
            return false;
        }

        if (!shellControl.isRunning(true)) {
            return false;
        }

        // If a subshell is active, then we are alive
        if (shellControl.isSubShellActive()) {
            return true;
        }

        // Don't run commands while in startup / exit
        if (shellControl.isInitializing() || shellControl.isExiting()) {
            return true;
        }

        try {
            // Don't print it constantly
            return shellControl
                    .command(CommandBuilder.of().add("echo", "xpipetest"))
                    .sensitive()
                    .executeAndCheck();
        } catch (Exception ex) {
            throw ErrorEventFactory.expected(ex);
        }
    }
}
