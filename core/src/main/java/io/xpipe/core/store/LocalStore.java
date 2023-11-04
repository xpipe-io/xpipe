package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.util.JacksonizedValue;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements ShellStore, StatefulDataStore<ShellStoreState> {

    private static ShellControl local;
    private static ShellControl localPowershell;
    private static FileSystem localFileSystem;

    public static void init() throws Exception {
        local = ProcessControlProvider.get().createLocalProcessControl(false).start();
        localFileSystem = new ConnectionFileSystem(ProcessControlProvider.get().createLocalProcessControl(false), new LocalStore());
    }

    public static ShellControl getLocalPowershell() throws Exception {
        if (localPowershell == null) {
            localPowershell = ProcessControlProvider.get().createLocalProcessControl(true).subShell(ShellDialects.POWERSHELL).start();
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
    public Class<ShellStoreState> getStateClass() {
        return ShellStoreState.class;
    }

    @Override
    public ShellControl control() {
        var pc = ProcessControlProvider.get().createLocalProcessControl(true);
        pc.withShellStateInit(this);
        pc.withShellStateFail(this);
        return pc;
    }
}
