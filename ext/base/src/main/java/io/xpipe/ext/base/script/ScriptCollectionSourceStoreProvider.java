package io.xpipe.ext.base.script;


import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.StoreStateFormat;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;

import java.util.List;
import java.util.UUID;

public class ScriptCollectionSourceStoreProvider implements DataStoreProvider {

    @Override
    public int getOrderPriority() {
        return 1;
    }

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        return DataStorage.SCRIPT_SOURCES_CATEGORY_UUID;
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.SCRIPTING;
    }

    @Override
    public boolean canMoveCategories() {
        return false;
    }

    @Override
    public boolean shouldShowScan() {
        return false;
    }

    @Override
    public BaseRegionBuilder<?,?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SCRIPT;
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        ScriptCollectionSourceStore st = store.getValue().asNeeded();

        var source = new SimpleObjectProperty<>(st.getSource());

        var sourceChoice = OptionsChoiceBuilder.builder().property(source).available(ScriptCollectionSource.getClasses()).build();

        return new OptionsBuilder()
                .nameAndDescription("scriptCollectionSourceType")
                .sub(sourceChoice.build(), source)
                .nonNull()
                .bind(
                        () -> {
                            return ScriptCollectionSourceStore.builder().source(source.get()).build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        ScriptCollectionSourceStore st = wrapper.getEntry().getStore().asNeeded();
        return st.getSource().toName();
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        ScriptCollectionSourceStore st = section.getWrapper().getEntry().getStore().asNeeded();
        return Bindings.createStringBinding(() -> {
            var s = st.getState();
            var summary = st.getSource().toSummary();
            var init = s.getEntries() != null;
            var format = new StoreStateFormat(List.of(), summary,
                    init ? AppI18n.get("scriptsContained", s.getEntries().size()) : null, !init ? AppI18n.get("notInitialized") : null);
            return format.format();
        }, section.getWrapper().getPersistentState(), AppI18n.activeLanguage());
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return ScriptCollectionSourceStore.builder().build();
    }

    @Override
    public String getId() {
        return "scriptCollectionSource";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ScriptCollectionSourceStore.class);
    }
}
