package io.xpipe.app.util;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;

import lombok.Getter;
import lombok.SneakyThrows;

public class LocalShell {

    @Getter
    private static LocalShellCache localCache;

    private static ShellControl local;
    private static ShellControl localPowershell;

    public static void init() throws Exception {
        local = ProcessControlProvider.get().createLocalProcessControl(false).start();
        localCache = new LocalShellCache(local);
    }

    public static void reset() {
        if (local != null) {
            local.kill();
            local = null;
        }
        localCache = null;
        if (localPowershell != null) {
            localPowershell.kill();
            localPowershell = null;
        }
    }

    public static ShellControl getLocalPowershell() throws Exception {
        var s = getShell();
        if (ShellDialects.isPowershell(s)) {
            return s;
        }

        if (localPowershell == null) {
            try {
                localPowershell = ProcessControlProvider.get()
                        .createLocalProcessControl(false)
                        .subShell(ShellDialects.POWERSHELL)
                        .start();
            } catch (ProcessOutputException ex) {
                throw ProcessOutputException.withPrefix("Failed to start local powershell process", ex);
            }
        }
        return localPowershell.start();
    }

    public static boolean isLocalShellInitialized() {
        return local != null;
    }

    @SneakyThrows
    public static ShellControl getShell() {
        if (local == null) {
            throw new IllegalStateException("Local shell not initialized yet");
        }

        return local.start();
    }

    public static ShellDialect getDialect() {
        return ProcessControlProvider.get().getEffectiveLocalDialect();
    }
}
