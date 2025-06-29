package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.FailableSupplier;

import lombok.Getter;

@Getter
public class ShellSession extends Session {

    private final FailableSupplier<ShellControl> supplier;
    private final ShellControl shellControl;

    public ShellSession(FailableSupplier<ShellControl> supplier) throws Exception {
        this.supplier = supplier;
        this.shellControl = createControl();
    }

    public void start() throws Exception {
        if (shellControl.isRunning(true)) {
            return;
        } else {
            stop();
        }

        try {
            shellControl.start();

            var supportsAliveCheck =
                    shellControl.getShellDialect().getDumbMode().supportsAnyPossibleInteraction();
            if (supportsAliveCheck) {
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

    private ShellControl createControl() throws Exception {
        var pc = supplier.get();
        pc.onStartupFail(shellControl -> {
            listener.onStateChange(false);
        });
        pc.onInit(shellControl -> {
            listener.onStateChange(true);
        });
        pc.onKill(() -> {
            listener.onStateChange(false);
        });
        // Listen for parent exit as onExit is called before exit is completed
        // In case it is stuck, we would not get the right status otherwise
        pc.getParentControl().ifPresent(p -> {
            p.onExit(shellControl -> {
                listener.onStateChange(false);
            });
        });
        pc.onExit(shellControl -> {
            listener.onStateChange(false);
        });
        return pc;
    }

    public boolean isRunning() {
        return shellControl.isRunning(true);
    }

    public void stop() throws Exception {
        shellControl.shutdown();
    }

    @Override
    public boolean checkAlive() throws Exception {
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
