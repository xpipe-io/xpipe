package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import io.xpipe.ext.base.host.HostAddressGatewayStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class FixedServiceStoreProvider extends AbstractServiceStoreProvider {

    @Override
    public DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        FixedServiceStore s = store.getStore().asNeeded();
        return DataStorage.get()
                .getOrCreateNewSyntheticEntry(
                        s.getDisplayParent().get(),
                        "Services",
                        FixedServiceGroupStore.builder()
                                .parent(s.getDisplayParent().asNeeded())
                                .build());
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        FixedServiceStore st = store.getValue().asNeeded();
        var host = new ReadOnlyObjectWrapper<>(st.getHost());
        var localPort = new SimpleObjectProperty<>(st.getLocalPort());
        var serviceProtocolType = new SimpleObjectProperty<>(st.getServiceProtocolType());
        var tunnelToLocalhost = new SimpleBooleanProperty(st.getTunnelToLocalhost() != null ? st.getTunnelToLocalhost() : true);
        var hideTunnelToLocalhost = Bindings.createBooleanBinding(() -> {
            return host.get() == null || (host.get().getStore() instanceof HostAddressGatewayStore g &&
                    g.getTunnelGateway() != null && !(g.getTunnelGateway().getStore() instanceof LocalStore));
        }, host);
        var hideLocalPort = Bindings.createBooleanBinding(() -> {
            return host.get() == null || !tunnelToLocalhost.get();
        }, hideTunnelToLocalhost, tunnelToLocalhost);

        var q = new OptionsBuilder()
                .nameAndDescription("serviceHost")
                .addComp(new StoreChoiceComp<>(entry, host, NetworkTunnelStore.class, n -> n.getStore().isLocallyTunnelable(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .disable()
                .sub(ServiceProtocolTypeHelper.choice(serviceProtocolType), serviceProtocolType)
                .nonNull()
                .nameAndDescription("serviceRemotePort")
                .addStaticString(st.getRemotePort())
                .nameAndDescription("tunnelToLocalhost")
                .addToggle(tunnelToLocalhost)
                .hide(hideTunnelToLocalhost)
                .nameAndDescription("serviceLocalPort")
                .addInteger(localPort)
                .hide(hideLocalPort)
                .bind(
                        () -> {
                            return FixedServiceStore.builder()
                                    .host(host.get())
                                    .displayParent(st.getDisplayParent())
                                    .localPort(localPort.get())
                                    .remotePort(st.getRemotePort())
                                    .serviceProtocolType(serviceProtocolType.get())
                                    .tunnelToLocalhost(tunnelToLocalhost.get())
                                    .build();
                        },
                        store);
        return q.buildDialog();
    }

    @Override
    public String getId() {
        return "fixedService";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(FixedServiceStore.class);
    }
}
