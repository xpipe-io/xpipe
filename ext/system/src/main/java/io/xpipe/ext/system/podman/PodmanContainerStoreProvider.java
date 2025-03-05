package io.xpipe.ext.system.podman;

import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ShellStoreFormat;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.service.FixedServiceGroupStore;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class PodmanContainerStoreProvider implements ShellStoreProvider {

    public void onParentRefresh(DataStoreEntry entry) {
        var services = FixedServiceGroupStore.builder().parent(entry.ref()).build();
        var servicesEntry = DataStorage.get().getStoreEntryIfPresent(services, false);
        if (servicesEntry.isPresent()) {
            DataStorage.get().refreshChildren(servicesEntry.get());
        }
    }

    @Override
    public boolean shouldShow(StoreEntryWrapper w) {
        PodmanContainerStore s = w.getEntry().getStore().asNeeded();
        var state = s.getState();
        return Boolean.TRUE.equals(state.getRunning())
                || s.getCmd().getStore().getState().isShowNonRunning();
    }

    @Override
    public boolean shouldShowScan() {
        return false;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        PodmanContainerStore s = store.getStore().asNeeded();
        return s.getCmd().get();
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        var val = new SimpleValidator();
        PodmanContainerStore st = (PodmanContainerStore) store.getValue();

        var q = new OptionsBuilder()
                .name("host")
                .description("podmanHostDescription")
                .addComp(StoreChoiceComp.host(
                        new SimpleObjectProperty<>(
                                st.getCmd() != null ? st.getCmd().getStore().getHost() : null),
                        StoreViewState.get().getAllConnectionsCategory()))
                .disable()
                .name("container")
                .description("podmanContainerDescription")
                .addString(new SimpleObjectProperty<>(st.getContainerName()), false)
                .disable()
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        PodmanContainerStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(
                        s.getCmd().getStore().getHost().get()) + " container";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        var c = (ContainerStoreState) section.getWrapper().getPersistentState().getValue();
        var missing = c.getShellMissing() != null && c.getShellMissing() ? "No shell available" : null;
        return ShellStoreFormat.shellStore(section, (ContainerStoreState s) -> new String[] {missing, s.getContainerState()});
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
}
