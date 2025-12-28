package io.xpipe.ext.system.incus;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.IdentityChoiceBuilder;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class IncusContainerStoreProvider implements ShellStoreProvider {

    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new OsLogoComp(w, BindingsHelper.map(w.getPersistentState(), o -> {
            var state = (ContainerStoreState) o;
            var cs = state.getContainerState();
            if (cs != null && cs.toLowerCase().contains("stopped")) {
                return SystemStateComp.State.FAILURE;
            } else if (cs != null && cs.toLowerCase().contains("running")) {
                return SystemStateComp.State.SUCCESS;
            } else {
                return SystemStateComp.State.OTHER;
            }
        }));
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        var c = (ContainerStoreState) section.getWrapper().getPersistentState().getValue();
        var missing = c.getShellMissing() != null && c.getShellMissing() ? "No shell available" : null;
        return StoreStateFormat.shellStore(
                section,
                (ContainerStoreState s) -> new String[] {missing, DataStoreFormatter.capitalize(s.getContainerState())},
                null);
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.LXC;
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
                .addComp(new StoreChoiceComp<>(
                        entry,
                        new ReadOnlyObjectWrapper<>(st.getInstall().getStore().getHost()),
                        ShellStore.class,
                        null,
                        StoreViewState.get().getAllConnectionsCategory()))
                .disable()
                .name("container")
                .description("lxdContainerDescription")
                .addStaticString(st.getContainerName())
                .sub(IdentityChoiceBuilder.container(identity), identity)
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
    public String getDisplayIconFileName(DataStore store) {
        return "system:lxd_icon.svg";
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
