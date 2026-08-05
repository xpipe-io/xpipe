package io.xpipe.ext.system.lxd;

import io.xpipe.app.hub.entry.*;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.store.DataStoreUsageCategory;
import io.xpipe.app.util.DocumentationLink;

import java.util.List;

public class LxdCmdStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.LXC;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var nonRunning = StoreToggleComp.<LxdCmdStore>childrenToggle(
                true, sec, s -> s.getState().isShowNonRunning(), (s, aBoolean) -> {
                    var state =
                            s.getState().toBuilder().showNonRunning(aBoolean).build();
                    s.setState(state);
                });
        return StoreEntryComp.create(sec, nonRunning, preferLarge);
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        LxdCmdStore s = store.getStore().asNeeded();
        return s.getHost();
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (LxdCmdStore) section.getEntry().getStore().asNeeded();
        var state = st.getState();
        return StoreEntryInformation.of(
                StoreEntryBadge.ofSuccess(state.isReachable() ? "LXD v" + state.getServerVersion() : null));
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
