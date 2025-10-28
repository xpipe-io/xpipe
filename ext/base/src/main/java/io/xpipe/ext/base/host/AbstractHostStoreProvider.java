package io.xpipe.ext.base.host;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.SneakyThrows;

import java.util.List;

public class AbstractHostStoreProvider implements DataStoreProvider {

    @Override
    public int getOrderPriority() {
        return 2;
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.HOST;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return Bindings.createStringBinding(
                () -> {
                    var all = section.getAllChildren().getList();
                    var shown = section.getShownChildren().getList();
                    if (shown.size() == 0) {
                        return null;
                    }

                    var string = all.size() == shown.size() ? all.size() : shown.size() + "/" + all.size();
                    return all.size() > 0
                            ? (all.size() == 1
                                    ? AppI18n.get("hostHasConnection", string)
                                    : AppI18n.get("ahostHasConnections", string))
                            : AppI18n.get("hostNoConnections");
                },
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                AppI18n.activeLanguage());
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        AbstractHostStore st = store.getValue().asNeeded();

        var host = new SimpleObjectProperty<>(st.getHost());
        var gateway = new SimpleObjectProperty<>(st.getGateway());

        return new OptionsBuilder()
                .nameAndDescription("abstractHostAddress")
                .addString(host)
                .nonNull()
                .nameAndDescription("abstractHostGateway")
                .addComp(
                        new StoreChoiceComp<>(
                                StoreChoiceComp.Mode.PROXY,
                                entry,
                                gateway,
                                NetworkTunnelStore.class,
                                ref -> true,
                                StoreViewState.get().getAllConnectionsCategory()),
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
}
