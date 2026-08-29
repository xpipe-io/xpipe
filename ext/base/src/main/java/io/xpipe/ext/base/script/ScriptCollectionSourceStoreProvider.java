package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreCreationCategory;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.GuiDialog;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

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
    public boolean shouldShowScan() {
        return false;
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SCRIPT_SOURCE;
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        ScriptCollectionSourceStore st = store.getValue().asNeeded();

        var source = new SimpleObjectProperty<>(st.getSource());

        var sourceChoice = OptionsChoiceBuilder.builder()
                .property(source)
                .available(ScriptCollectionSource.getClasses())
                .build();

        return new OptionsBuilder()
                .nameAndDescription("scriptCollectionSourceType")
                .sub(sourceChoice.build(), source)
                .nonNull()
                .bind(
                        () -> {
                            return ScriptCollectionSourceStore.builder()
                                    .source(source.get())
                                    .build();
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
    public StoreEntryInformation buildInformation(StoreSection section) {
        ScriptCollectionSourceStore st = section.getEntry().getStore().asNeeded();
        var s = st.getState();
        var init = s.getEntries() != null;
        var count = init ? AppI18n.get("scriptsContained", s.getEntries().size()) : null;
        return StoreEntryInformation.of(
                StoreEntryBadge.ofFile(st.getSource().toSummary()),
                count != null
                        ? (s.getEntries().size() > 0
                                ? StoreEntryBadge.ofSuccess(count)
                                : StoreEntryBadge.ofFailure(count))
                        : null,
                StoreEntryBadge.ofFailure(!init ? AppI18n.get("notInitialized") : null));
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
