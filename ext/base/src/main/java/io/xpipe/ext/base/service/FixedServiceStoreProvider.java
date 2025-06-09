package io.xpipe.ext.base.service;

import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.Property;
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
    public String getId() {
        return "fixedService";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(FixedServiceStore.class);
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        FixedServiceStore st = store.getValue().asNeeded();
        var host = new SimpleObjectProperty<>(st.getHost());
        var localPort = new SimpleObjectProperty<>(st.getLocalPort());
        var serviceProtocolType = new SimpleObjectProperty<>(st.getServiceProtocolType());
        var q = new OptionsBuilder()
                .nameAndDescription("serviceHost")
                .addComp(
                        StoreChoiceComp.other(
                                host,
                                NetworkTunnelStore.class,
                                n -> n.getStore().isLocallyTunnelable(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .sub(ServiceProtocolTypeHelper.choice(serviceProtocolType), serviceProtocolType)
                .nonNull()
                .nameAndDescription("serviceLocalPort")
                .addInteger(localPort)
                .bind(
                        () -> {
                            return FixedServiceStore.builder()
                                    .host(host.get())
                                    .displayParent(st.getDisplayParent())
                                    .localPort(localPort.get())
                                    .remotePort(st.getRemotePort())
                                    .serviceProtocolType(serviceProtocolType.get())
                                    .build();
                        },
                        store);
        return q.buildDialog();
    }
}
