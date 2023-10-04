package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.storage.store.DenseStoreEntryComp;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.comp.storage.store.StoreSection;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class ScriptGroupStoreProvider implements DataStoreProvider {

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public Comp<?> customEntryComp(StoreSection s, boolean preferLarge) {
        return new DenseStoreEntryComp(s.getWrapper(), true, null);
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
