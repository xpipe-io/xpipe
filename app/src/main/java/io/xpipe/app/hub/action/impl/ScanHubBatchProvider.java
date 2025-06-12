package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.BatchHubProvider;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ScanDialog;
import io.xpipe.app.util.ScanDialogAction;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ScanHubBatchProvider implements BatchHubProvider<ShellStore> {

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
    public ObservableValue<String> getName() {
        return AppI18n.observable("addConnections");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2l-layers-plus");
    }

    @Override
    public Class<?> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public String getId() {
        return "scanStoreBatch";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends MultiStoreAction<ShellStore> {

        @Override
        public void executeImpl() {
            ScanDialog.showMulti(refs, ScanDialogAction.shellScanAction());
        }
    }
}
