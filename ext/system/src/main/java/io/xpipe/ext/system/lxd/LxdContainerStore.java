package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.*;
import io.xpipe.app.process.BaseElevationHandler;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.host.HostAddressGatewayStore;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

@JsonTypeName("lxd")
@SuperBuilder(toBuilder = true)
@Jacksonized
@Getter
@AllArgsConstructor
@Value
public class LxdContainerStore
        implements ShellStore,
                FixedChildStore,
                StatefulDataStore<NetworkContainerStoreState>,
                StartableStore,
                StoppableStore,
                PauseableStore,
                NameableStore,
                HostAddressGatewayStore {

    DataStoreEntryRef<LxdCmdStore> cmd;
    String projectName;
    String containerName;
    IdentityValue identity;

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return DataStoreDependencies.of(cmd, identity != null ? identity.getDependencies() : null);
    }

    @Override
    public Class<NetworkContainerStoreState> getStateClass() {
        return NetworkContainerStoreState.class;
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
        if (projectName != null) {
            return OptionalInt.of(Objects.hash(projectName, containerName));
        } else {
            return OptionalInt.of(Objects.hash(containerName));
        }
    }

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (LxdContainerStore) other;
        return toBuilder()
                .identity(identity != null ? identity : o.identity)
                .projectName(o.projectName)
                .build();
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlParentStoreFunction() {

            @Override
            public ShellControl control(ShellControl parent) throws Exception {
                refreshContainerState(getCmd().getStore().getHost().getStore().getOrStartSession());

                var user = identity != null ? identity.unwrap().getUsername().retrieveUsername() : null;
                var sc = new LxdCommandView(parent).exec(projectName, containerName, user, () -> {
                    var state = getState();
                    var alpine = state.getOsName() != null
                            && state.getOsName().toLowerCase().contains("alpine");
                    return alpine;
                });
                sc.withSourceStore(LxdContainerStore.this);
                if (identity != null && identity.unwrap().getPassword() != null) {
                    sc.setElevationHandler(new BaseElevationHandler(
                                    LxdContainerStore.this, identity.unwrap().getPassword())
                            .orElse(sc.getElevationHandler()));
                }
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

            @Override
            public ShellStore getParentStore() {
                return getCmd().getStore().getHost().getStore();
            }
        };
    }

    private void refreshContainerState(ShellControl sc) throws Exception {
        var state = getState();
        var view = new LxdCommandView(sc);
        var displayState = view.queryContainerState(projectName, containerName);
        if (displayState.isEmpty()) {
            return;
        }

        var running = "Running".equals(displayState.get().getState());
        var newState = state.toBuilder()
                .containerState(displayState.get().getState())
                .running(running)
                .ipv4(displayState.get().getIpv4Address())
                .ipv6(displayState.get().getIpv6Address())
                .build();
        setState(newState);
    }

    @Override
    public void start() throws Exception {
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.start(projectName, containerName);
        refreshContainerState(sc);
    }

    @Override
    public void stop() throws Exception {
        stopSessionIfNeeded();
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.stop(projectName, containerName);
        refreshContainerState(sc);
    }

    @Override
    public void pause() throws Exception {
        var sc = getCmd().getStore().getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        view.pause(projectName, containerName);
        refreshContainerState(sc);
    }

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getTunnelGateway() {
        var host = getCmd().getStore().getHost();
        return host.getStore() instanceof NetworkTunnelStore ? host.asNeeded() : null;
    }

    @Override
    public void refreshHostAddressOrThrow() throws Exception {
        refreshContainerState(getCmd().getStore().getHost().getStore().getOrStartSession());
    }

    @Override
    public HostAddress getHostAddress() {
        var l = new ArrayList<String>();
        var state = getState();
        if (state.getIpv4() != null) {
            l.add(state.getIpv4());
        }
        if (state.getIpv6() != null) {
            l.add(state.getIpv6());
        }
        return !l.isEmpty() ? HostAddress.of(l) : HostAddress.empty();
    }
}
