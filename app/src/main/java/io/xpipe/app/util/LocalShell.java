package io.xpipe.app.util;

import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import lombok.Getter;

public class LocalShell {

    @Getter
    private static ShellControlCache localCache;
    private static ShellControl local;
    private static ShellControl localPowershell;

    public static void init() {
        local = ProcessControlProvider.get().createLocalProcessControl(false).start();
        localCache = new ShellControlCache(local);
    }

    public static ShellControl getLocalPowershell() {
        if (localPowershell == null) {
            localPowershell = ProcessControlProvider.get().createLocalProcessControl(true)
                    .subShell(ShellDialects.POWERSHELL)
                    .start();
        }
        return localPowershell;
    }

    public static boolean isLocalShellInitialized() {
        return local != null;
    }

    public static ShellControl getShell() {
        if (local == null) {
            throw new IllegalStateException("Local shell not initialized yet");
        }

        return local;
    }
}
