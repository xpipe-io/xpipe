package io.xpipe.ext.system.podman;

import io.xpipe.app.ext.*;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.InternalCacheDataStore;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.ext.base.SelfReferentialStore;
import io.xpipe.ext.base.service.AbstractServiceStore;
import io.xpipe.ext.base.service.FixedServiceCreatorStore;
import io.xpipe.ext.base.service.MappedServiceStore;
import io.xpipe.ext.base.store.StartableStore;
import io.xpipe.ext.base.store.StoppableStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Pattern;

@JsonTypeName("podman")
@SuperBuilder
@Jacksonized
@Value
public class PodmanContainerStore
        implements StartableStore,
                   StoppableStore,
                   ShellStore,
                   InternalCacheDataStore,
                   FixedChildStore,
                   StatefulDataStore<ContainerStoreState>,
                   FixedServiceCreatorStore,
                   SelfReferentialStore,
                   ContainerImageStore, NameableStore {

    DataStoreEntryRef<PodmanCmdStore> cmd;
    String containerName;

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public String getImageName() {
        return getState().getImageName();
    }

    public PodmanCommandView.Container commandView(ShellControl parent) {
        return new PodmanCommandView(parent).container();
    }

    @Override
    public void start() throws Exception {
        var view = commandView(getCmd().getStore().getHost().getStore().getOrStartSession());
        view.start(containerName);
        var state = getState().toBuilder().running(true).containerState("Up").build();
        setState(state);
    }

    @Override
    public void stop() throws Exception {
        var view = commandView(getCmd().getStore().getHost().getStore().getOrStartSession());
        view.stop(containerName);
        var state =
                getState().toBuilder().running(false).containerState("Exited").build();
        setState(state);
    }

    @Override
    public List<? extends DataStoreEntryRef<? extends AbstractServiceStore>> createFixedServices() throws Exception {
        return findServices().stream()
                .map(s -> DataStoreEntry.createNew("Service", s).<MappedServiceStore>ref())
                .toList();
    }

    private List<MappedServiceStore> findServices() throws Exception {
        var entry = getSelfEntry();
        var view = commandView(getCmd().getStore().getHost().getStore().getOrStartSession());
        var out = view.port(containerName);
        return out.lines()
                .map(l -> {
                    var matcher = Pattern.compile("(\\d+)/\\w+\\s*->\\s*[^:]+?:(\\d+)")
                            .matcher(l);
                    if (!matcher.matches()) {
                        return (MappedServiceStore) null;
                    }

                    var containerPort = Integer.parseInt(matcher.group(1));
                    var remotePort = Integer.parseInt(matcher.group(2));
                    return MappedServiceStore.builder()
                            .host(getCmd().getStore().getHost().asNeeded())
                            .displayParent(entry.ref())
                            .containerPort(containerPort)
                            .remotePort(remotePort)
                            .build();
                })
                .filter(dockerServiceStore -> dockerServiceStore != null)
                .toList();
    }

    @Override
    public Class<ContainerStoreState> getStateClass() {
        return ContainerStoreState.class;
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(Objects.hash(containerName));
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(cmd);
        Validators.isType(cmd, PodmanCmdStore.class);
        cmd.checkComplete();
        Validators.nonNull(containerName);
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlParentStoreFunction() {

            @Override
            public ShellStore getParentStore() {
                return getCmd().getStore().getHost().getStore();
            }

            @Override
            public ShellControl control(ShellControl parent) {
                var pc = new PodmanCommandView(parent).container().exec(containerName);
                pc.withSourceStore(PodmanContainerStore.this);
                pc.withShellStateInit(PodmanContainerStore.this);
                pc.onInit(shellControl -> {
                    var s = getState().toBuilder()
                            .osType(shellControl.getOsType())
                            .shellDialect(shellControl.getShellDialect())
                            .ttyState(shellControl.getTtyState())
                            .running(true)
                            .osName(shellControl.getOsName())
                            .build();
                    setState(s);
                });
                pc.onStartupFail(throwable -> {
                    if (throwable instanceof LicenseRequiredException) {
                        return;
                    }

                    var stateBuilder = getState().toBuilder();
                    stateBuilder.running(false);
                    var hasShell = throwable.getMessage() == null
                            || !throwable.getMessage().contains("OCI runtime exec failed");
                    if (!hasShell) {
                        stateBuilder.containerState("No shell available");
                    } else {
                        stateBuilder.containerState("Connection failed");
                    }
                    setState(stateBuilder.build());
                });
                return pc;
            }
        };
    }
}
