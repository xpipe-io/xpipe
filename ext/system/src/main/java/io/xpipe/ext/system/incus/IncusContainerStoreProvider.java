package io.xpipe.ext.system.incus;

import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.*;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.*;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.IdentityChoiceBuilder;
import io.xpipe.ext.base.store.ShellStoreProvider;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class IncusContainerStoreProvider implements ShellStoreProvider {

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (IncusContainerStore) section.getEntry().getStore().asNeeded();
        var state = st.getState();
        var parentInfo = ShellStoreProvider.super.buildInformation(section);
        var addr = HostAddress.of(state.getIpv4(), state.getIpv6());
        return parentInfo.append(StoreEntryInformation.of(
                StoreEntryBadge.ofRunningState(state.getContainerState(), "Running", "Stopped"),
                StoreEntryBadge.ofDynamicAddress(addr),
                StoreEntryBadge.ofFailure(
                        state.getShellMissing() != null && state.getShellMissing() ? "No shell available" : null)));
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
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        IncusContainerStore s = store.getStore().asNeeded();
        return s.getInstall();
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        IncusContainerStore st = (IncusContainerStore) store.getValue();
        var identity = new SimpleObjectProperty<>(st.getIdentity());

        var q = new OptionsBuilder()
                .name("container")
                .description("containerDescription")
                .addStaticString((st.getProjectName() != null ? st.getProjectName() + "/" : "") + st.getContainerName())
                .sub(IdentityChoiceBuilder.container(identity, model.getSyncable()), identity)
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
