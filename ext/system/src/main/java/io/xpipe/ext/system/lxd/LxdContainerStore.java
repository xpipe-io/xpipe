package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.ShellControlFunction;
import io.xpipe.app.ext.ShellControlParentStoreFunction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.StatefulDataStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.ext.base.identity.IdentityValue;
import io.xpipe.ext.base.store.PauseableStore;
import io.xpipe.ext.base.store.StartableStore;
import io.xpipe.ext.base.store.StoppableStore;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("lxd")
@SuperBuilder
@Jacksonized
@Value
@AllArgsConstructor
public class LxdContainerStore implements ShellStore, FixedChildStore, StatefulDataStore<ContainerStoreState>, StartableStore, StoppableStore, PauseableStore {

    private final DataStoreEntryRef<LxdCmdStore> cmd;
    private final String containerName;
    IdentityValue identity;

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
            identity.checkComplete(false, false, false);
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
                Integer uid = null;
                FilePath homeDir = null;
                ShellDialect shell;
                try (var temp = new LxdCommandView(parent)
                        .exec(containerName, null, null, ShellDialects.SH)
                        .start()) {
                    if (identity != null && identity.unwrap().getUsername() != null) {
                        var passwd = PasswdFile.parse(temp);
                        var username = identity.unwrap().getUsername();
                        uid = passwd.getUidForUserIfPresent(username)
                                .orElseThrow(() -> new IllegalArgumentException("User " + username + " not found"));
                        homeDir = temp.command("eval echo ~" +username).readStdoutIfPossible().filter(s -> !s.isBlank())
                                .map(FilePath::new).orElse(null);
                    }
                    shell = CommandSupport.isInPath(temp, "bash") ? ShellDialects.BASH : ShellDialects.SH;
                }

                var base = new LxdCommandView(parent).exec(containerName, uid, homeDir, shell);
                if (identity != null && identity.unwrap().getPassword() != null) {
                    base.setElevationHandler(new BaseElevationHandler(
                            LxdContainerStore.this, identity.unwrap().getPassword())
                            .orElse(base.getElevationHandler()));
                }
                return base.withSourceStore(LxdContainerStore.this)
                        .onInit(shellControl -> {
                            var s = getState().toBuilder()
                                    .osType(shellControl.getOsType())
                                    .shellDialect(shellControl.getShellDialect())
                                    .ttyState(shellControl.getTtyState())
                                    .running(true)
                                    .osName(shellControl.getOsName())
                                    .build();
                            setState(s);
                        })
                        .onStartupFail(throwable -> {
                            if (throwable instanceof LicenseRequiredException) {
                                return;
                            }

                            var s = getState().toBuilder()
                                    .running(false)
                                    .containerState("Connection failed")
                                    .build();
                            setState(s);
                        });
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
