package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class MappedServiceStoreProvider extends FixedServiceStoreProvider {

    public String displayName(DataStoreEntry entry) {
        MappedServiceStore s = entry.getStore().asNeeded();
        return DataStorage.get().getStoreEntryDisplayName(s.getHost().get()) + " - Port " + s.getContainerPort();
    }

    protected String formatService(AbstractServiceStore s) {
        var m = (MappedServiceStore) s;
        var desc = s.getLocalPort() != null
                ? "localhost:" + s.getLocalPort() + " <- :" + m.getRemotePort() + " <- :" + m.getContainerPort()
                : s.isSessionRunning()
                        ? "localhost:" + s.getSession().getLocalPort() + " <- :" + m.getRemotePort() + " <- :"
                                + m.getContainerPort()
                        : ":" + m.getRemotePort() + " <- :" + m.getContainerPort();
        return desc;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        MappedServiceStore st = store.getValue().asNeeded();
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
                .nameAndDescription("serviceRemotePort")
                .addStaticString(st.getRemotePort() + " <- " + st.getContainerPort())
                .nameAndDescription("serviceLocalPort")
                .addInteger(localPort)
                .bind(
                        () -> {
                            return MappedServiceStore.builder()
                                    .host(host.get())
                                    .displayParent(st.getDisplayParent())
                                    .localPort(localPort.get())
                                    .remotePort(st.getRemotePort())
                                    .serviceProtocolType(serviceProtocolType.get())
                                    .containerPort(st.getContainerPort())
                                    .build();
                        },
                        store);
        return q.buildDialog();
    }

    @Override
    public String getId() {
        return "mappedService";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MappedServiceStore.class);
    }
}
