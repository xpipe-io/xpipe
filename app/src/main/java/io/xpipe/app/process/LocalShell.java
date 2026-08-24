package io.xpipe.app.process;

import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.issue.ErrorEventFactory;

import io.xpipe.app.util.FailableConsumer;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LocalShell {

    private static ShellControl local;
    private static ShellControl localPowershell;
    private static boolean powershellInitialized;
    private static final Map<Object, ShellControl> localShellInstances = new HashMap<>();

    public static synchronized ShellControl get(Object key) throws Exception {
        var found = localShellInstances.get(key);
        if (found != null) {
            return found.start();
        }

        var sc = ProcModuleProvider.get().createLocalProcessControl(true).start();
        localShellInstances.put(key, sc);
        return sc;
    }

    public static synchronized ShellControl get(Object key, FailableConsumer<ShellControl, Exception> func) throws Exception {
        var found = localShellInstances.get(key);
        if (found != null) {
            var wasRunning = found.isRunning(true);
            if (!wasRunning) {
                found.start();
                func.accept(found);
            }
            return found;
        }

        var sc = ProcModuleProvider.get().createLocalProcessControl(true).start();
        func.accept(sc);
        localShellInstances.put(key, sc);
        return sc;
    }

    public static synchronized boolean isInitialized() {
        return local != null;
    }

    public static synchronized ShellControl init() throws Exception {
        if (local == null) {
            local = ProcModuleProvider.get().createLocalProcessControl(false).start();
        }
        return local;
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

        try {
            if (powershellInitialized) {
                return Optional.ofNullable(localPowershell != null ? localPowershell.start() : null);
            }

            powershellInitialized = true;
            localPowershell = ProcModuleProvider.get()
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
        return ProcModuleProvider.get().getEffectiveLocalDialect();
    }
}
