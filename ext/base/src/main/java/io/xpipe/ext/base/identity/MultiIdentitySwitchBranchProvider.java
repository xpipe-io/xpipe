package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.stream.Collectors;

public class MultiIdentitySwitchBranchProvider implements HubBranchProvider<MultiIdentityStore> {

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<MultiIdentityStore> store) {
        var selected = store.getStore().getSelected();
        return store.getStore().getAvailableIdentities().stream()
                .map(is -> {
                    return new IdentityProvider(
                            is, selected.map(ref -> ref.equals(is)).orElse(false));
                })
                .collect(Collectors.toList());
    }

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CONFIGURATION;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<MultiIdentityStore> o) {
        return o.getStore().getAvailableIdentities().size() > 1;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<MultiIdentityStore> store) {
        return AppI18n.observable("switchIdentity");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<MultiIdentityStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-format-list-group");
    }

    @Override
    public Class<MultiIdentityStore> getApplicableClass() {
        return MultiIdentityStore.class;
    }

    private static class IdentityProvider implements HubLeafProvider<MultiIdentityStore> {

        private final DataStoreEntryRef<IdentityStore> identity;
        private final boolean active;

        private IdentityProvider(DataStoreEntryRef<IdentityStore> identity, boolean active) {
            this.identity = identity;
            this.active = active;
        }

        @Override
        public void execute(DataStoreEntryRef<MultiIdentityStore> ref) {
            ThreadHelper.runAsync(() -> {
                ref.getStore().select(identity);
            });
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<MultiIdentityStore> store) {
            return new ReadOnlyStringWrapper(
                    (active ? "> " : "") + identity.get().getName());
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<MultiIdentityStore> store) {
            return new LabelGraphic.ImageGraphic(identity.get().getEffectiveIconFile(), 16);
        }

        @Override
        public Class<MultiIdentityStore> getApplicableClass() {
            return MultiIdentityStore.class;
        }
    }
}
