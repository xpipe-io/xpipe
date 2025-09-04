package io.xpipe.ext.system.lxd;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class LxdCmdStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.LXC;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var nonRunning = StoreToggleComp.<LxdCmdStore>childrenToggle(
                null, true, sec, s -> s.getState().isShowNonRunning(), (s, aBoolean) -> {
                    var state =
                            s.getState().toBuilder().showNonRunning(aBoolean).build();
                    s.setState(state);
                });
        return StoreEntryComp.create(sec, nonRunning, preferLarge);
    }

    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(BindingsHelper.map(w.getPersistentState(), o -> {
            var state = (LxdCmdStore.State) o;
            if (state.isReachable()) {
                return SystemStateComp.State.SUCCESS;
            }

            return SystemStateComp.State.FAILURE;
        }));
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        LxdCmdStore s = store.getStore().asNeeded();
        return s.getHost().get();
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var state = (LxdCmdStore.State) o;
            return state.isReachable() ? "lxd v" + state.getServerVersion() : "Connection failed";
        });
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "system:lxd_icon.svg";
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return new LxdCmdStore(DataStorage.get().local().ref());
    }

    @Override
    public String getId() {
        return "lxdCmd";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LxdCmdStore.class);
    }
}
