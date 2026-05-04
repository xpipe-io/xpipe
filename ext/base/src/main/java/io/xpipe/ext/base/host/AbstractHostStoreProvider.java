package io.xpipe.ext.base.host;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.SneakyThrows;

import java.util.List;

public class AbstractHostStoreProvider implements CountGroupStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.ABSTRACT_HOSTS;
    }

    @Override
    public int getOrderPriority() {
        return 2;
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.HOST;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        AbstractHostStore st = store.getValue().asNeeded();

        var host = new SimpleObjectProperty<>(st.getHost());
        var gateway = new SimpleObjectProperty<>(st.getTunnelGateway());

        return new OptionsBuilder()
                .nameAndDescription("abstractHostAddress")
                .addString(host)
                .nonNull()
                .nameAndDescription("abstractHostGateway")
                .addComp(
                        new StoreChoiceComp<>(
                                model.getExistingEntry(),
                                gateway,
                                NetworkTunnelStore.class,
                                null,
                                StoreViewState.get().getAllConnectionsCategory(),
                                DataStoreCreationCategory.HOST),
                        gateway)
                .bind(
                        () -> {
                            return AbstractHostStore.builder()
                                    .host(host.getValue())
                                    .gateway(gateway.getValue())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        AbstractHostStore scriptStore = wrapper.getEntry().getStore().asNeeded();
        return scriptStore.getHost();
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:abstractHost_icon.svg";
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return AbstractHostStore.builder().build();
    }

    @Override
    public String getId() {
        return "abstractHost";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(AbstractHostStore.class);
    }

    @Override
    public String getCountTranslationKey() {
        return "Connection";
    }
}
