package io.xpipe.ext.system.podman;

import io.xpipe.app.hub.entry.*;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.store.DataStoreUsageCategory;
import io.xpipe.app.util.DocumentationLink;

import java.util.List;

public class PodmanCmdStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.PODMAN;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var nonRunning = StoreToggleComp.<PodmanCmdStore>childrenToggle(
                true, sec, s -> s.getState().isShowNonRunning(), (s, aBoolean) -> {
                    s.setState(s.getState().toBuilder().showNonRunning(aBoolean).build());
                });
        return StoreEntryComp.create(sec, nonRunning, preferLarge);
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        PodmanCmdStore s = store.getStore().asNeeded();
        return s.getHost();
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (PodmanCmdStore) section.getEntry().getStore().asNeeded();
        var state = st.getState();
        var v = state.isRunning()
                ? (state.getServerName() != null ? state.getServerName() : "Podman") + " v" + state.getVersion()
                : null;
        return StoreEntryInformation.of(
                state.isRunning() ? StoreEntryBadge.ofSuccess(v) : StoreEntryBadge.ofFailure(v));
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "system:podman_icon.svg";
    }

    @Override
    public String getId() {
        return "podmanCmd";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(PodmanCmdStore.class);
    }
}
