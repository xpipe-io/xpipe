package io.xpipe.app.ext;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.vnc.VncBaseStore;

import java.util.ServiceLoader;

public abstract class ProcessControlProvider {

    private static ProcessControlProvider INSTANCE;

    public static void init(ModuleLayer layer) {
        INSTANCE = ServiceLoader.load(layer, ProcessControlProvider.class).stream()
                .map(p -> p.get())
                .findFirst()
                .orElseThrow();
    }

    public abstract BrowserStoreSessionTab<?> createVncSession(
            BrowserFullSessionModel model, DataStoreEntryRef<VncBaseStore> ref);

    public static ProcessControlProvider get() {
        return INSTANCE;
    }

    public abstract DataStoreEntryRef<ShellStore> elevated(DataStoreEntryRef<ShellStore> e);

    public abstract void reset();

    public abstract ShellControl withDefaultScripts(ShellControl pc);

    public abstract CommandControl command(ShellControl parent, CommandBuilder command, CommandBuilder terminalCommand);

    public abstract ShellControl createLocalProcessControl(boolean stoppable);

    public abstract Object getStorageSyncHandler();

    public abstract Object getStorageUserHandler();

    public abstract ShellDialect getEffectiveLocalDialect();

    public abstract void toggleFallbackShell();

    public abstract ShellDialect getDefaultLocalDialect();

    public abstract ShellDialect getFallbackDialect();

    public abstract <T extends DataStore> DataStoreEntryRef<T> replace(DataStoreEntryRef<T> ref);
}
