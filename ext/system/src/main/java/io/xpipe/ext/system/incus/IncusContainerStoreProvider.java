package io.xpipe.ext.system.incus;

import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.identity.IdentityChoice;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class IncusContainerStoreProvider implements ShellStoreProvider {

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "system:lxd_icon.svg";
    }

    @Override
    public boolean shouldShow(StoreEntryWrapper w) {
        IncusContainerStore s = w.getEntry().getStore().asNeeded();
        var state = s.getState();
        return Boolean.TRUE.equals(state.getRunning())
                || s.getInstall().getStore().getState().isShowNonRunning();
    }

    @Override
    public boolean shouldShowScan() {
        return false;
    }

    public String createInsightsMarkdown(DataStore store) {
        var c = (IncusContainerStore) store;
        return String.format(
                """
                    XPipe will execute:
                    ```
                    %s
                    ```
                    in a host shell of `%s` to open a shell into the container.
                    """,
                new IncusCommandView(null)
                        .execCommand(c.getContainerName(), true)
                        .buildSimple(),
                c.getInstall().getStore().getHost().get().getName());
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        IncusContainerStore s = store.getStore().asNeeded();
        return s.getInstall().get();
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        IncusContainerStore st = (IncusContainerStore) store.getValue();
        var identity = new SimpleObjectProperty<>(st.getIdentity());

        var q = new OptionsBuilder()
                .name("host")
                .description("lxdHostDescription")
                .addComp(StoreChoiceComp.host(
                        new SimpleObjectProperty<>(st.getInstall().getStore().getHost()),
                        StoreViewState.get().getAllConnectionsCategory()))
                .disable()
                .name("container")
                .description("lxdContainerDescription")
                .addString(new SimpleObjectProperty<>(st.getContainerName()), false)
                .disable()
                .sub(IdentityChoice.container(identity), identity)
                .bind(
                        () -> {
                            return IncusContainerStore.builder()
                                    .containerName(st.getContainerName())
                                    .install(st.getInstall())
                                    .identity(identity.getValue())
                                    .build();
                        },
                        store)
                .buildDialog();
        return q;
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        IncusContainerStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(
                        s.getInstall().getStore().getHost().get()) + " container";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return ShellStoreFormat.shellStore(
                section, (ContainerStoreState s) -> DataStoreFormatter.capitalize(s.getContainerState()));
    }

    @Override
    public String getId() {
        return "incusContainer";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(IncusContainerStore.class);
    }
}
