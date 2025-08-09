package io.xpipe.app.hub.action.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.HostAddressSwitchStore;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.stream.Collectors;

public class HostAddressSwitchBranchProvider implements HubBranchProvider<HostAddressSwitchStore> {

    private static class HostAddressProvider implements HubLeafProvider<HostAddressSwitchStore> {

        private final boolean active;
        private final String address;

        private HostAddressProvider(boolean active, String address) {
            this.active = active;
            this.address = address;}

        @Override
        public void execute(DataStoreEntryRef<HostAddressSwitchStore> ref) {
            var newStore = ref.getStore().withAddress(address);
            if (newStore.isPresent()) {
                ref.get().setStoreInternal(newStore.get(), true);
            }
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<HostAddressSwitchStore> store) {
            return new ReadOnlyStringWrapper(address);
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<HostAddressSwitchStore> store) {
            return active ? new LabelGraphic.IconGraphic("mdi2a-arrow-right") : LabelGraphic.none();
        }

        @Override
        public Class<HostAddressSwitchStore> getApplicableClass() {
            return HostAddressSwitchStore.class;
        }
    }

    @Override
    public List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<HostAddressSwitchStore> store) {
        return store.getStore().getHostAddress().getAvailable().stream().map(s -> {
            return new HostAddressProvider(s.equals(store.getStore().getHostAddress().get()), s);
        }).collect(Collectors.toList());
    }

    @Override
    public Class<HostAddressSwitchStore> getApplicableClass() {
        return HostAddressSwitchStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<HostAddressSwitchStore> o) {
        return !o.getStore().getHostAddress().isSingle();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<HostAddressSwitchStore> store) {
        return AppI18n.observable("switchHostAddress");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<HostAddressSwitchStore> store) {
        return new LabelGraphic.IconGraphic("mdi2f-format-list-group");
    }

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CONFIGURATION;
    }
}
