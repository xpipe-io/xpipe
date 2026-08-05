package io.xpipe.ext.system.incus;

import io.xpipe.app.process.BaseElevationHandler;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellControlFunction;
import io.xpipe.app.process.ShellControlParentStoreFunction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.*;
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

@JsonTypeName("incusContainer")
@SuperBuilder(toBuilder = true)
@Jacksonized
@Getter
@AllArgsConstructor
@Value
public class IncusContainerStore
        implements ShellStore,
                FixedChildStore,
                StatefulDataStore<NetworkContainerStoreState>,
                StartableStore,
                StoppableStore,
                PauseableStore,
                NameableStore,
                HostAddressGatewayStore {

    DataStoreEntryRef<IncusInstallStore> install;
    String projectName;
    String containerName;
    IdentityValue identity;

    public IncusCommandView.Project view() throws Exception {
        return view(getInstall().getStore().getHost().getStore().getOrStartSession());
    }

    public IncusCommandView.Project view(ShellControl sc) {
        return new IncusCommandView(sc).project(projectName);
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return DataStoreDependencies.of(install, identity != null ? identity.getDependencies() : null);
    }

    @Override
    public Class<NetworkContainerStoreState> getStateClass() {
        return NetworkContainerStoreState.class;
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
        if (projectName != null) {
            return OptionalInt.of(Objects.hash(projectName, containerName));
        } else {
            return OptionalInt.of(Objects.hash(containerName));
        }
    }

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (IncusContainerStore) other;
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
                refreshContainerState();

                var user = identity != null ? identity.unwrap().getUsername().retrieveUsername() : null;
                var sc = view(parent).exec(containerName, user, () -> {
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

    private void refreshContainerState() throws Exception {
        var state = getState();
        var view = view();
        var displayState = view.queryContainerState(containerName);
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
        var view = view();
        view.start(containerName);
        // Give the network some time to initialize so the state
        // gets updated to include any IP address
        ThreadHelper.sleep(500);
        refreshContainerState();
    }

    @Override
    public void stop() throws Exception {
        stopSessionIfNeeded();
        var view = view();
        view.stop(containerName);
        refreshContainerState();
    }

    @Override
    public void pause() throws Exception {
        var view = view();
        view.pause(containerName);
        refreshContainerState();
    }

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public DataStoreEntryRef<NetworkTunnelStore> getTunnelGateway() {
        var host = getInstall().getStore().getHost();
        return host.getStore() instanceof NetworkTunnelStore ? host.asNeeded() : null;
    }

    @Override
    public void refreshHostAddressOrThrow() throws Exception {
        refreshContainerState();
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
