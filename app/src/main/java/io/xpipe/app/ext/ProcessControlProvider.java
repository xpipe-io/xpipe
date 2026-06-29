package io.xpipe.app.ext;

import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HttpProxy;
import io.xpipe.app.util.RemoteDesktopDockContentEntry;
import io.xpipe.app.vnc.VncBaseStore;
import io.xpipe.app.util.SecretValue;

import javafx.beans.property.Property;

import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

public abstract class ProcessControlProvider {

    private static ProcessControlProvider INSTANCE;

    public static void init(ModuleLayer layer) {
        INSTANCE = ServiceLoader.load(layer, ProcessControlProvider.class).stream()
                .map(p -> p.get())
                .findFirst()
                .orElseThrow();
    }

    public static ProcessControlProvider get() {
        return INSTANCE;
    }

    public abstract String generatePublicSshKey(SecretValue privateKey, SecretRetrievalStrategy passphrase);

    public abstract void showSshKeygenDialog(String commentDefault, Property<?> identityProperty);

    public abstract ShellStore subShellEnvironment(DataStoreEntryRef<ShellStore> s, ShellDialect dialect);

    public abstract RemoteDesktopDockContentEntry createVncSession(
            DataStoreEntryRef<VncBaseStore> ref, Runnable onKill);

    public abstract DataStoreEntryRef<ShellStore> elevated(DataStoreEntryRef<ShellStore> e);

    public abstract void reset();

    public abstract ShellControl withDefaultScripts(ShellControl pc);

    public abstract CommandControl command(ShellControl parent, CommandBuilder command, CommandBuilder terminalCommand);

    public abstract ShellControl createLocalProcessControl(boolean stoppable);

    public abstract Object getStorageSyncHandler();

    public abstract Object getStorageUserHandler();

    public abstract ShellDialect getEffectiveLocalDialect();

    public abstract McpSchema.CallToolResult executeMcpCommand(ShellControl sc, String command) throws Exception;

    public ShellDialect getNextFallbackDialect() {
        var av = getAvailableLocalDialects();
        var index = av.indexOf(getEffectiveLocalDialect());
        var next = (index + 1) % av.size();
        return av.get(next);
    }

    public abstract void toggleFallbackShell();

    public abstract List<ShellDialect> getAvailableLocalDialects();

    public abstract <T extends DataStore> DataStoreEntryRef<T> replace(DataStoreEntryRef<T> ref);

    public abstract ModalOverlay createNetworkScanModal();

    public abstract void cloneRepository(String url, Path target) throws Exception;

    public abstract void pullRepository(Path target) throws Exception;

    public abstract Optional<HttpProxy> getHttpProxy(DataStoreEntryRef<?> store);

    public abstract void addAskpassEnvironment(
            CommandBuilder b, String prefix, UUID requestId, UUID secretId, String... askpassName);

    public abstract void refreshWsl();

    public abstract void showWebtopDeploymentDialog();

    public abstract void showLocalWebtopMobileConnectDialog();

    public abstract void importRdpFile(Path file) throws Exception;
}
