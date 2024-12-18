package io.xpipe.ext.system.podman;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.*;
import io.xpipe.ext.base.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.regex.Pattern;

@JsonTypeName("podmanCmd")
@SuperBuilder
@Jacksonized
@Value
public class PodmanCmdStore
        implements FixedHierarchyStore, StatefulDataStore<PodmanCmdStore.State>, SelfReferentialStore {

    private final DataStoreEntryRef<ShellStore> host;

    public PodmanCmdStore(DataStoreEntryRef<ShellStore> host) {
        this.host = host;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(host);
        Validators.isType(host, ShellStore.class);
        host.checkComplete();
    }

    private List<DataStoreEntryRef<PodmanContainerStore>> listContainers(ShellControl sc) throws Exception {
        var view = new PodmanCommandView(sc);
        var l = view.container().listContainersAndStates();
        return l.stream()
                .map(s -> {
                    boolean running =
                            s.getStatus().startsWith("running") || s.getStatus().startsWith("up");
                    var c = PodmanContainerStore.builder()
                            .cmd(getSelfEntry().ref())
                            .containerName(s.getName())
                            .build();
                    var entry = DataStoreEntry.createNew(s.getName(), c);
                    entry.setStorePersistentState(ContainerStoreState.builder()
                            .containerState(s.getStatus())
                            .imageName(s.getImage())
                            .running(running)
                            .build());
                    return entry.<PodmanContainerStore>ref();
                })
                .toList();
    }

    private void updateState(ShellControl host) throws Exception {
        var out = new PodmanCommandView(host).version();

        var namePattern = Pattern.compile("Server:\\s+(.+)");
        var nameMatcher = namePattern.matcher(out);
        var name = nameMatcher.find() ? nameMatcher.group(1) : null;

        var versionPattern = Pattern.compile("Version:\\s+(.+)");
        var versionMatcher = versionPattern.matcher(out);
        var version = versionMatcher.find() ? versionMatcher.group(1) : null;

        setState(getState().toBuilder()
                .running(true)
                .serverName(name)
                .version(version)
                .build());
    }

    @Override
    public List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren() throws Exception {
        var sc = getHost().getStore().getOrStartSession();
        var view = new PodmanCommandView(sc);
        CommandSupport.isSupported(() -> view.isSupported(), "Podman CLI", host.get());
        var running = view.isDaemonRunning();
        if (!running) {
            setState(getState().toBuilder().running(false).build());
            throw ErrorEvent.expected(new IllegalStateException("Podman daemon is not running"));
        }

        updateState(sc);
        return listContainers(sc);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(toBuilder = true)
    @Jacksonized
    public static class State extends DataStoreState {
        String serverName;
        String version;
        boolean running;
        boolean showNonRunning;
    }
}
