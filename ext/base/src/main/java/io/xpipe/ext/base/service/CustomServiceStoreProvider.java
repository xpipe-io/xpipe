package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreComboChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.ext.base.host.AbstractHostStore;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class CustomServiceStoreProvider extends AbstractServiceStoreProvider {

    @Override
    public DataStoreEntry getSyntheticParent(DataStoreEntry store) {
        var c = (CustomServiceStore) store.getStore();
        if (c.getHost() == null || c.getHost().getStore() instanceof AbstractHostStore) {
            return null;
        }

        return super.getSyntheticParent(store);
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        var c = (CustomServiceStore) store.getStore();
        if (c.getHost() != null && c.getHost().getStore() instanceof AbstractHostStore) {
            return c.getHost().get();
        }

        return super.getDisplayParent(store);
    }

    @Override
    public int getOrderPriority() {
        return -1;
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SERVICE;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        CustomServiceStore st = store.getValue().asNeeded();

        var comboHost = new SimpleObjectProperty<>(StoreComboChoiceComp.ComboValue.of(st.getAddress(), st.getHost()));
        var gateway = new SimpleObjectProperty<>(st.getGateway());
        var hideGateway = BindingsHelper.map(comboHost, c -> c == null || c.getRef() != null);

        var localPort = new SimpleObjectProperty<>(st.getLocalPort());
        var remotePort = new SimpleObjectProperty<>(st.getRemotePort());
        var serviceProtocolType = new SimpleObjectProperty<>(st.getServiceProtocolType());

        var hostChoice = new StoreComboChoiceComp<>(
                hostStore -> hostStore instanceof AbstractHostStore a
                        ? a.getHostAddress().get()
                        : hostStore instanceof NetworkTunnelStore t ? t.getTunnelHostName() : "?",
                entry,
                comboHost,
                NetworkTunnelStore.class,
                n -> n.getStore() instanceof AbstractHostStore
                        || (n.getStore() instanceof NetworkTunnelStore t && t.isLocallyTunnelable()),
                StoreViewState.get().getAllConnectionsCategory());
        var gatewayChoice = new StoreChoiceComp<>(
                StoreChoiceComp.Mode.PROXY,
                entry,
                gateway,
                NetworkTunnelStore.class,
                ref -> !ref.get().equals(DataStorage.get().local()),
                StoreViewState.get().getAllConnectionsCategory());

        var q = new OptionsBuilder()
                .nameAndDescription("serviceHost")
                .addComp(hostChoice, comboHost)
                .nonNull()
                .nameAndDescription("gateway")
                .addComp(gatewayChoice, gateway)
                .hide(hideGateway)
                .nameAndDescription("serviceRemotePort")
                .addInteger(remotePort)
                .nonNull()
                .sub(ServiceProtocolTypeHelper.choice(serviceProtocolType), serviceProtocolType)
                .nonNull()
                .nameAndDescription("serviceLocalPort")
                .addInteger(localPort)
                .bind(
                        () -> {
                            return CustomServiceStore.builder()
                                    .address(
                                            comboHost.get() != null
                                                    ? comboHost.get().getManualHost()
                                                    : null)
                                    .host(
                                            comboHost.get() != null
                                                    ? comboHost.get().getRef()
                                                    : null)
                                    .gateway(gateway.get())
                                    .localPort(localPort.get())
                                    .remotePort(remotePort.get())
                                    .serviceProtocolType(serviceProtocolType.get())
                                    .build();
                        },
                        store);
        return q.buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return CustomServiceStore.builder().build();
    }

    @Override
    public String getId() {
        return "customService";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(CustomServiceStore.class);
    }
}
