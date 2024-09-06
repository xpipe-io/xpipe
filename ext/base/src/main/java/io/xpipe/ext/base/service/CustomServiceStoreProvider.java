package io.xpipe.ext.base.service;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.NetworkTunnelStore;
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
        return DataStoreCreationCategory.TUNNEL;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        CustomServiceStore st = store.getValue().asNeeded();
        var host = new SimpleObjectProperty<>(st.getHost());
        var localPort = new SimpleObjectProperty<>(st.getLocalPort());
        var remotePort = new SimpleObjectProperty<>(st.getRemotePort());

        var q = new OptionsBuilder()
                .nameAndDescription("serviceHost")
                .addComp(
                        DataStoreChoiceComp.other(
                                host,
                                NetworkTunnelStore.class,
                                n -> n.getStore().isLocallyTunneable(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .nameAndDescription("serviceRemotePort")
                .addInteger(remotePort)
                .nonNull()
                .nameAndDescription("serviceLocalPort")
                .addInteger(localPort)
                .bind(
                        () -> {
                            return CustomServiceStore.builder()
                                    .host(host.get())
                                    .localPort(localPort.get())
                                    .remotePort(remotePort.get())
                                    .build();
                        },
                        store);
        return q.buildDialog();
    }

    @Override
    public DataStore defaultStore() {
        return CustomServiceStore.builder().build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("customService");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(CustomServiceStore.class);
    }
}
