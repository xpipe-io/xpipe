package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.DropdownComp;
import io.xpipe.app.comp.base.StoreToggleComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.DenseStoreEntryComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;

import java.util.List;

public class ScriptGroupStoreProvider implements DataStoreProvider {

    @Override
    public Comp<?> customEntryComp(StoreSection sec, boolean preferLarge) {
        ScriptGroupStore s = sec.getWrapper().getEntry().getStore().asNeeded();
        if (sec.getWrapper().getValidity().getValue() != DataStoreEntry.Validity.COMPLETE) {
            return new DenseStoreEntryComp(sec.getWrapper(), true, null);
        }

        var def = new StoreToggleComp("base.isDefaultGroup", sec, s.getState().isDefault(), aBoolean -> {
            var state = s.getState();
            state.setDefault(aBoolean);
            s.setState(state);
        });

        var bring = new StoreToggleComp("base.bringToShells", sec, s.getState().isBringToShell(), aBoolean -> {
            var state = s.getState();
            state.setBringToShell(aBoolean);
            s.setState(state);
        });

        var dropdown = new DropdownComp(List.of(def, bring));
        return new DenseStoreEntryComp(sec.getWrapper(), true, dropdown);
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        ScriptGroupStore st = store.getValue().asNeeded();

        var group = new SimpleObjectProperty<>(st.getGroup());
        Property<String> description = new SimpleObjectProperty<>(st.getDescription());
        return new OptionsBuilder()
                .name("description")
                .description("descriptionDescription")
                .addString(description)
                .name("scriptGroup")
                .description("scriptGroupDescription")
                .addComp(
                        new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.OTHER, entry, group, ScriptGroupStore.class, null, StoreViewState.get().getAllScriptsCategory()),
                        group)
                .bind(
                        () -> {
                            return ScriptGroupStore.builder()
                                    .group(group.get())
                                    .description(st.getDescription())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        ScriptGroupStore scriptStore = store.getStore().asNeeded();
        return scriptStore.getParent() != null ? scriptStore.getParent().get() : null;
    }

    @Override
    public DataStore defaultStore() {
        return ScriptGroupStore.builder().build();
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.SCRIPT;
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
            return "proc:shellEnvironment_icon.svg";
    }

    @Override
    public boolean canHaveSubShells() {
        return false;
    }

    @Override
    public ObservableValue<String> informationString(StoreEntryWrapper wrapper) {
        ScriptGroupStore scriptStore = wrapper.getEntry().getStore().asNeeded();
        return new SimpleStringProperty(scriptStore.getDescription());
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("scriptGroup");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ScriptGroupStore.class);
    }

}
