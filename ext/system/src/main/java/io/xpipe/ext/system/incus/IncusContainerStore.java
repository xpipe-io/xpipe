package io.xpipe.ext.system.incus;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.ShellControlFunction;
import io.xpipe.app.ext.ShellControlParentStoreFunction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CountDown;
import io.xpipe.core.process.ElevationHandler;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.StatefulDataStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.SecretReference;
import io.xpipe.ext.base.store.PauseableStore;
import io.xpipe.ext.base.store.StartableStore;
import io.xpipe.ext.base.store.StoppableStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

@JsonTypeName("incusContainer")
@SuperBuilder
@Jacksonized
@Getter
@AllArgsConstructor
@Value
public class IncusContainerStore implements ShellStore, FixedChildStore, StatefulDataStore<ContainerStoreState>, StartableStore, StoppableStore, PauseableStore {

    DataStoreEntryRef<IncusInstallStore> install;
    String containerName;
    String user;
    SecretRetrievalStrategy password;

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
                return getInstall().getStore().getHost().getStore();
            }

            @Override
            public ShellControl control(ShellControl parent) throws Exception {
                Integer uid = null;
                if (user != null) {
                    try (var temp = new IncusCommandView(parent).exec(containerName, null).start()) {
                        var passwd = PasswdFile.parse(temp);
                        uid = passwd.getUidForUser(user);
                    }
                }

                var sc = new IncusCommandView(parent).exec(containerName, uid);
                sc.withSourceStore(IncusContainerStore.this);
                sc.setElevationHandler(new BaseElevationHandler(IncusContainerStore.this, password).orElse(sc.getElevationHandler()));
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
        };
    }

    private void refreshContainerState(ShellControl sc) throws Exception {
        var state = getState();
        var view = new IncusCommandView(sc);
        var displayState = view.queryContainerState(containerName);
        var running = "RUNNING".equals(displayState);
        var newState = state.toBuilder().containerState(displayState).running(running).build();
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
}
