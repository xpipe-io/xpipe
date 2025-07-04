package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.*;
import io.xpipe.app.ext.FixedChildStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.IdentityValue;
import io.xpipe.ext.base.store.PauseableStore;
import io.xpipe.ext.base.store.StartableStore;
import io.xpipe.ext.base.store.StoppableStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("lxd")
@SuperBuilder(toBuilder = true)
@Jacksonized
@Value
@AllArgsConstructor
public class LxdContainerStore
        implements ShellStore,
                FixedChildStore,
                StatefulDataStore<ContainerStoreState>,
                StartableStore,
                StoppableStore,
                PauseableStore,
                NameableStore {

    DataStoreEntryRef<LxdCmdStore> cmd;
    String containerName;
    IdentityValue identity;

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (LxdContainerStore) other;
        return toBuilder().identity(identity != null ? identity : o.identity).build();
    }

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public Class<ContainerStoreState> getStateClass() {
        return ContainerStoreState.class;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(cmd);
        Validators.isType(cmd, LxdCmdStore.class);
        cmd.checkComplete();
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
    public ShellControlFunction shellFunction() {
        return new ShellControlParentStoreFunction() {

            @Override
            public ShellStore getParentStore() {
                return getCmd().getStore().getHost().getStore();
            }

            @Override
            public ShellControl control(ShellControl parent) throws Exception {
                refreshContainerState(getCmd().getStore().getHost().getStore().getOrStartSession());

                var user = identity != null ? identity.unwrap().getUsername().retrieveUsername() : null;
                var sc = new LxdCommandView(parent).exec(containerName, user, () -> {
                    var state = getState();
                    var alpine = state.getOsName() != null
                            && state.getOsName().toLowerCase().contains("alpine");
                    return alpine;
                });
                if (identity != null && identity.unwrap().getPassword() != null) {
                    sc.setElevationHandler(new BaseElevationHandler(
                                    LxdContainerStore.this, identity.unwrap().getPassword())
                            .orElse(sc.getElevationHandler()));
                }
                sc.withSourceStore(LxdContainerStore.this);
                sc.withShellStateInit(LxdContainerStore.this);
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
        };
    }

    private void refreshContainerState(ShellControl sc) throws Exception {
        var state = getState();
        var view = new LxdCommandView(sc);
        var displayState = view.queryContainerState(containerName);
        var running = "RUNNING".equals(displayState);
        var newState =
                state.toBuilder().containerState(displayState).running(running).build();
        setState(newState);
    }

    @Override
    public void start() throws Exception {
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.start(containerName);
        refreshContainerState(sc);
    }

    @Override
    public void stop() throws Exception {
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.stop(containerName);
        refreshContainerState(sc);
    }

    @Override
    public void pause() throws Exception {
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.pause(containerName);
        refreshContainerState(sc);
    }
}
