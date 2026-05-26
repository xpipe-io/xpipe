package io.xpipe.ext.system.lxd;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.ext.NetworkContainerStoreState;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.IdentityChoiceBuilder;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class LxdContainerStoreProvider implements ShellStoreProvider {

    public BaseRegionBuilder<?, ?> stateDisplay(StoreSection section) {
        return new OsLogoComp(
                section.getWrapper(), BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
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
        return StoreStateFormat.shellStore(
                section,
                (NetworkContainerStoreState s) -> {
                    var missing = s.getShellMissing() != null && s.getShellMissing() ? "No shell available" : null;
                    return new String[] {missing, DataStoreFormatter.capitalize(s.getContainerState()), s.getIpv4()};
                },
                null);
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.LXC;
    }

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
        return s.getCmd() != null ? s.getCmd().get() : null;
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        LxdContainerStore st = (LxdContainerStore) store.getValue();
        var identity = new SimpleObjectProperty<>(st.getIdentity());

        var q = new OptionsBuilder()
                .name("container")
                .description("containerDescription")
                .addStaticString((st.getProjectName() != null ? st.getProjectName() + "/" : "") + st.getContainerName())
                .sub(IdentityChoiceBuilder.container(identity, model.getSyncable()), identity)
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
    public String getId() {
        return "lxd";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LxdContainerStore.class);
    }
}
