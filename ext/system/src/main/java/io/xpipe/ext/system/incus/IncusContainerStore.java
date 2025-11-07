package io.xpipe.ext.system.incus;

import io.xpipe.app.ext.*;
import io.xpipe.app.process.BaseElevationHandler;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.IdentityValue;
import io.xpipe.ext.base.store.PauseableStore;
import io.xpipe.ext.base.store.StartableStore;
import io.xpipe.ext.base.store.StoppableStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("incusContainer")
@SuperBuilder(toBuilder = true)
@Jacksonized
@Getter
@AllArgsConstructor
@Value
public class IncusContainerStore
        implements ShellStore,
                FixedChildStore,
                StatefulDataStore<ContainerStoreState>,
                StartableStore,
                StoppableStore,
                PauseableStore,
                NameableStore {

    DataStoreEntryRef<IncusInstallStore> install;
    String containerName;
    IdentityValue identity;

    @Override
    public Class<ContainerStoreState> getStateClass() {
        return ContainerStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(install);
        Validators.isType(install, IncusInstallStore.class);
        install.checkComplete();
        Validators.nonNull(containerName);
        if (identity != null) {
            identity.checkComplete();
        }
    }

    @Override
    public OptionalInt getFixedId() {
        return OptionalInt.of(Objects.hash(containerName));
    }

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (IncusContainerStore) other;
        return toBuilder().identity(identity != null ? identity : o.identity).build();
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlParentStoreFunction() {

            @Override
            public ShellControl control(ShellControl parent) throws Exception {
                refreshContainerState(
                        getInstall().getStore().getHost().getStore().getOrStartSession());

                var user = identity != null ? identity.unwrap().getUsername().retrieveUsername() : null;
                var sc = new IncusCommandView(parent).exec(containerName, user, () -> {
                    var state = getState();
                    var alpine = state.getOsName() != null
                            && state.getOsName().toLowerCase().contains("alpine");
                    return alpine;
                });
                sc.withSourceStore(IncusContainerStore.this);
                if (identity != null && identity.unwrap().getPassword() != null) {
                    sc.setElevationHandler(new BaseElevationHandler(
                                    IncusContainerStore.this, identity.unwrap().getPassword())
                            .orElse(sc.getElevationHandler()));
                }
                sc.withShellStateInit(IncusContainerStore.this);
                sc.onStartupFail(throwable -> {
                    if (throwable instanceof LicenseRequiredException) {
                        return;
                    }

                    var s = getState().toBuilder()
                            .running(false)
                            .containerState("Connection failed")
                            .build();
                    setState(s);
                });

                return sc;
            }

            @Override
            public ShellStore getParentStore() {
                return getInstall().getStore().getHost().getStore();
            }
        };
    }

    private void refreshContainerState(ShellControl sc) throws Exception {
        var state = getState();
        var view = new IncusCommandView(sc);
        var displayState = view.queryContainerState(containerName);
        var running = "RUNNING".equals(displayState);
        var newState =
                state.toBuilder().containerState(displayState).running(running).build();
        setState(newState);
    }

    @Override
    public void start() throws Exception {
        var sc = getInstall().getStore().getHost().getStore().getOrStartSession();
        var view = new IncusCommandView(sc);
        view.start(containerName);
        refreshContainerState(sc);
    }

    @Override
    public void stop() throws Exception {
        var sc = getInstall().getStore().getHost().getStore().getOrStartSession();
        var view = new IncusCommandView(sc);
        view.stop(containerName);
        refreshContainerState(sc);
    }

    @Override
    public void pause() throws Exception {
        var sc = getInstall().getStore().getHost().getStore().getOrStartSession();
        var view = new IncusCommandView(sc);
        view.pause(containerName);
        refreshContainerState(sc);
    }

    @Override
    public String getName() {
        return containerName;
    }
}
