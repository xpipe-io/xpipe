package io.xpipe.ext.system.podman;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.hub.comp.StoreEntryComp;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.StoreToggleComp;
import io.xpipe.app.hub.comp.SystemStateComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class PodmanCmdStoreProvider implements DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.PODMAN;
    }

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        var nonRunning = StoreToggleComp.<PodmanCmdStore>childrenToggle(
                null, true, sec, s -> s.getState().isShowNonRunning(), (s, aBoolean) -> {
                    s.setState(s.getState().toBuilder().showNonRunning(aBoolean).build());
                });
        return StoreEntryComp.create(sec, nonRunning, preferLarge);
    }

    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(BindingsHelper.map(w.getPersistentState(), o -> {
            var state = (PodmanCmdStore.State) o;
            if (state.isRunning()) {
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
        PodmanCmdStore s = store.getStore().asNeeded();
        return s.getHost().get();
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var state = (PodmanCmdStore.State) o;
            if (!state.isRunning()) {
                return "Connection failed";
            }

            return (state.getServerName() != null ? state.getServerName() : "Podman") + " v" + state.getVersion();
        });
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
