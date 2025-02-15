package io.xpipe.ext.system.lxd;

import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ShellStoreFormat;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.identity.IdentityChoice;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class LxdContainerStoreProvider implements ShellStoreProvider {

    @Override
    public boolean shouldShow(StoreEntryWrapper w) {
        LxdContainerStore s = w.getEntry().getStore().asNeeded();
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
        LxdContainerStore s = store.getStore().asNeeded();
        return s.getCmd().get();
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        LxdContainerStore st = (LxdContainerStore) store.getValue();
        var identity = new SimpleObjectProperty<>(st.getIdentity());

        var q = new OptionsBuilder()
                .name("host")
                .description("lxdHostDescription")
                .addComp(StoreChoiceComp.host(
                        new SimpleObjectProperty<>(st.getCmd().getStore().getHost()),
                        StoreViewState.get().getAllConnectionsCategory()))
                .disable()
                .name("container")
                .description("lxdContainerDescription")
                .addString(new SimpleObjectProperty<>(st.getContainerName()), false)
                .disable()
                .sub(IdentityChoice.container(identity), identity)
                .bind(
                        () -> {
                            return LxdContainerStore.builder()
                                    .containerName(st.getContainerName())
                                    .cmd(st.getCmd())
                                    .identity(identity.getValue())
                                    .build();
                        },
                        store)
                .buildDialog();
        return q;
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        LxdContainerStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(
                        s.getCmd().getStore().getHost().get()) + " container";
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        return ShellStoreFormat.shellStore(
                section, (ContainerStoreState s) -> DataStoreFormatter.capitalize(s.getContainerState()));
    }

    @Override
    public String getId() {
        return "lxd";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LxdContainerStore.class);
    }
}
