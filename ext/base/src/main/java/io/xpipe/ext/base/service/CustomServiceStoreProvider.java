package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class CustomServiceStoreProvider extends AbstractServiceStoreProvider {

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
        var host = new SimpleObjectProperty<>(st.getHost());
        var localPort = new SimpleObjectProperty<>(st.getLocalPort());
        var remotePort = new SimpleObjectProperty<>(st.getRemotePort());
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
                                    .host(host.get())
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
