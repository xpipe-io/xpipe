package io.xpipe.ext.base.host;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostAddress;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class CopyIpActionProvider implements HubLeafProvider<HostAddressStore> {

    @Override
    public AbstractAction createAction(DataStoreEntryRef<HostAddressStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<HostAddressStore> store) {
        return AppI18n.observable("copyIp");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<HostAddressStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-clipboard-list-outline");
    }

    @Override
    public Class<?> getApplicableClass() {
        return HostAddressStore.class;
    }

    @Override
    public String getId() {
        return "copyIp";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<HostAddressStore> {

        @Override
        public void executeImpl() throws Exception {
            ref.getStore().refreshHostAddressOrThrow();
            var addr = ref.getStore().getHostAddress();
            if (addr == null || addr.equals(HostAddress.empty())) {
                throw ErrorEventFactory.expected(new IllegalStateException("System does not have a last known IP"));
            } else {
                var effective = addr.getIpv4Address().orElse(addr.get());
                ClipboardHelper.copyText(effective);
            }
        }
    }
}
