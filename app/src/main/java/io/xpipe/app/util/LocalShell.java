package io.xpipe.app.util;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;

import lombok.SneakyThrows;

import java.util.Optional;

public class LocalShell {

    private static ShellControl local;
    private static ShellControl localPowershell;
    private static boolean powershellInitialized;

    public static synchronized void init() throws Exception {
        local = ProcessControlProvider.get().createLocalProcessControl(false).start();
    }

    public static synchronized void reset(boolean force) {
        if (local != null) {
            if (!force) {
                try {
                    local.exitAndWait();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                    local.kill();
                }
            } else {
                local.kill();
            }
            local = null;
        }
        if (localPowershell != null) {
            if (!force) {
                try {
                    localPowershell.exitAndWait();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                    localPowershell.kill();
                }
            } else {
                localPowershell.kill();
            }
            localPowershell = null;
        }
    }

    public static synchronized Optional<ShellControl> getLocalPowershell() {
        if (local != null && ShellDialects.isPowershell(local)) {
            return Optional.of(local);
        }

        if (powershellInitialized) {
            return Optional.ofNullable(localPowershell);
        }

        try {
            powershellInitialized = true;
            localPowershell = ProcessControlProvider.get()
                    .createLocalProcessControl(false)
                    .subShell(ShellDialects.POWERSHELL)
                    .start();
            localPowershell.getShellDialect().getDumbMode().throwIfUnsupported();
        } catch (Exception ex) {
            localPowershell = null;
            ErrorEventFactory.fromThrowable(ex)
                    .description("Failed to start local powershell process")
                    .handle();
        }

        return Optional.ofNullable(localPowershell);
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
