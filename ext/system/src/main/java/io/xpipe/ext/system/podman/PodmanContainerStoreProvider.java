package io.xpipe.ext.system.podman;

import io.xpipe.app.hub.creation.StoreChoiceComp;
import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.*;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreCreationCategory;
import io.xpipe.app.store.ShellStore;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.service.FixedServiceGroupStore;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;

public class PodmanContainerStoreProvider implements ShellStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.PODMAN;
    }

    @Override
    public boolean shouldShow(StoreEntryWrapper w) {
        PodmanContainerStore s = w.getEntry().getStore().asNeeded();
        var state = s.getState();
        return Boolean.TRUE.equals(state.getRunning())
                || s.getCmd() == null
                || s.getCmd().getStore().getState().isShowNonRunning();
    }

    public void onParentRefresh(DataStoreEntry entry) {
        var services = FixedServiceGroupStore.builder().parent(entry.ref()).build();
        var servicesEntry = DataStorage.get().getStoreEntryIfPresent(services, false);
        if (servicesEntry.isPresent()) {
            DataStorage.get().refreshChildren(servicesEntry.get());
        }
    }

    @Override
    public boolean shouldShowScan() {
        return false;
    }

    @Override
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        PodmanContainerStore s = store.getStore().asNeeded();
        return s.getCmd();
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        PodmanContainerStore st = (PodmanContainerStore) store.getValue();

        return new OptionsBuilder()
                .name("host")
                .description("podmanHostDescription")
                .addComp(new StoreChoiceComp<>(
                        model.getExistingEntry(),
                        new ReadOnlyObjectWrapper<>(
                                st.getCmd() != null ? st.getCmd().getStore().getHost() : null),
                        ShellStore.class,
                        null,
                        StoreViewState.get().getAllConnectionsCategory(),
                        DataStoreCreationCategory.HOST))
                .disable()
                .name("container")
                .description("podmanContainerDescription")
                .addStaticString(st.getContainerName())
                .buildDialog();
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "system:podman_icon.svg";
    }

    @Override
    public String getId() {
        return "podman";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(PodmanContainerStore.class);
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (PodmanContainerStore) section.getEntry().getStore().asNeeded();
        var state = st.getState();
        var parentInfo = ShellStoreProvider.super.buildInformation(section);
        var cs = state.getContainerState();
        var running = cs != null && cs.toLowerCase().contains("up");
        var exited = cs != null && cs.toLowerCase().contains("exited");
        return parentInfo.append(StoreEntryInformation.of(
                StoreEntryBadge.ofRunningState(
                        running ? "Up" : exited ? "Exited": cs, running, exited),
                StoreEntryBadge.ofFailure(
                        state.getShellMissing() != null && state.getShellMissing() ? "No shell available" : null)));
    }
}
