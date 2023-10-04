package io.xpipe.ext.base.script;

import io.xpipe.app.comp.storage.store.DenseStoreEntryComp;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.comp.storage.store.StoreSection;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.fxcomps.impl.DataStoreListChoiceComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.Identifiers;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public class MultiScriptStoreProvider implements DataStoreProvider {

    @Override
    public Comp<?> customEntryComp(StoreSection s, boolean preferLarge) {
        return new DenseStoreEntryComp(s.getWrapper(),true, null);
    }

    @Override
    public boolean alwaysShowSummary() {
        return true;
    }

    @Override
    public boolean shouldHaveChildren() {
        return false;
    }

    @Override
    public boolean shouldEdit() {
        return true;
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.SCRIPT;
    }

    @Override
    public String getId() {
        return "multiScript";
    }

    @SneakyThrows
    @Override
    public String getDisplayIconFileName(DataStore store) {
            return "proc:shellEnvironment_icon.svg";
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        MultiScriptStore st = store.getValue().asNeeded();
        var group = new SimpleObjectProperty<>(st.getGroup());
        var others = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>(st.getEffectiveScripts())));
        return new OptionsBuilder()
                .name("scriptGroup")
                .description("scriptGroupDescription")
                .addComp(DataStoreChoiceComp.other(group, ScriptGroupStore.class, null), group)
                .name("snippets")
                .description("snippetsDependenciesDescription")
                .addComp(new DataStoreListChoiceComp<>(others, ScriptStore.class, scriptStore -> !scriptStore.get().equals(entry) && others.stream().noneMatch(scriptStoreDataStoreEntryRef -> scriptStoreDataStoreEntryRef.getStore().equals(scriptStore))), others)
                .nonEmpty()
                .bind(
                        () -> {
                            return MultiScriptStore.builder().group(group.get()).scripts(others.get()).description(st.getDescription()).build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
        MultiScriptStore st = wrapper.getEntry().getStore().asNeeded();
        return new SimpleStringProperty(st.getDescription());
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        MultiScriptStore st = store.getStore().asNeeded();
        return st.getGroup().get();
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MultiScriptStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return MultiScriptStore.builder().scripts(List.of()).build();
    }

    @Override
    public List<String> getPossibleNames() {
        return Identifiers.get("multiScript");
    }
}
