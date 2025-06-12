package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ScanDialog;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ScanHubLeafProvider implements HubLeafProvider<ShellStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.OPEN;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<ShellStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
        return true;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
        var state = o.get().getStorePersistentState();
        if (state instanceof SystemState systemState) {
            return (systemState.getShellDialect() == null
                            || systemState.getShellDialect().getDumbMode().supportsAnyPossibleInteraction())
                    && (systemState.getTtyState() == null || systemState.getTtyState() == ShellTtyState.NONE);
        } else {
            return true;
        }
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
        return AppI18n.observable("scanConnections");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
        return new LabelGraphic.IconGraphic("mdi2l-layers-plus");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public String getId() {
        return "scanStore";
    }

    @Jacksonized
    @SuperBuilder
    static class Action extends StoreAction<ShellStore> {

        @Override
        public void executeImpl() {
            ScanDialog.showSingleAsync(ref.get());
        }
    }
}
