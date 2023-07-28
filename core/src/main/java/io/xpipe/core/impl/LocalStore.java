package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements ShellStore {

    private static ShellControl local;
    private static ShellControl localPowershell;
    private static FileSystem localFileSystem;

    public static void init() throws Exception {
        local = ProcessControlProvider.createLocal(false).start();
        localFileSystem = new LocalStore().createFileSystem();
    }

    public static ShellControl getLocalPowershell() throws Exception {
        if (localPowershell == null) {
            localPowershell = new LocalStore()
                    .control()
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

    public static FileSystem getFileSystem() {
        if (localFileSystem == null) {
            throw new IllegalStateException("Local file system not initialized yet");
        }

        return localFileSystem;
    }

    @Override
    public ShellControl createBasicControl() {
        return ProcessControlProvider.createLocal(true);
    }
}
