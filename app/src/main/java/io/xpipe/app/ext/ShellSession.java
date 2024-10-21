package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.Session;
import io.xpipe.core.store.SessionListener;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class ShellSession extends Session {

    private final Supplier<ShellControl> supplier;
    private final ShellControl shellControl;

    public ShellSession(SessionListener listener, Supplier<ShellControl> supplier) {
        super(listener);
        this.supplier = supplier;
        this.shellControl = createControl();
    }

    public void start() throws Exception {
        if (shellControl.isRunning()) {
            return;
        } else {
            stop();
        }

        try {
            shellControl.start();
        } catch (Exception ex) {
            stop();
            throw ex;
        }
    }

    private ShellControl createControl() {
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
        return pc;
    }

    public boolean isRunning() {
        return shellControl.isRunning();
    }

    public void stop() throws Exception {
        shellControl.close();
    }
}
