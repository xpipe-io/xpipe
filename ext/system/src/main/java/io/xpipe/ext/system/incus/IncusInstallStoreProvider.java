package io.xpipe.ext.system.incus;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.core.store.DataStore;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class IncusInstallStoreProvider implements DataStoreProvider {

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var nonRunning = StoreToggleComp.<IncusInstallStore>childrenToggle(
                null, true, sec, s -> s.getState().isShowNonRunning(), (s, aBoolean) -> {
                    var state =
                            s.getState().toBuilder().showNonRunning(aBoolean).build();
                    s.setState(state);
                });
        return StoreEntryComp.create(sec, nonRunning, preferLarge);
    }

    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(BindingsHelper.map(w.getPersistentState(), o -> {
            var state = (IncusInstallStore.State) o;
            if (state.isReachable()) {
                return SystemStateComp.State.SUCCESS;
            }

            return SystemStateComp.State.FAILURE;
        }));
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        IncusInstallStore s = store.getStore().asNeeded();
        return s.getHost().get();
    }

    public String summaryString(StoreEntryWrapper wrapper) {
        IncusInstallStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getHost().get()) + " containers";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var state = (IncusInstallStore.State) o;
            return state.isReachable() ? "incus v" + state.getServerVersion() : "Connection failed";
        });
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "system:lxd_icon.svg";
    }

    @Override
    public DataStore defaultStore() {
        return new IncusInstallStore(DataStorage.get().local().ref());
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("incusInstall");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(IncusInstallStore.class);
    }
}