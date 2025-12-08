package io.xpipe.ext.base.identity;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.ShellTtyState;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.identity.ssh.NoIdentityStrategy;
import javafx.beans.value.ObservableValue;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class IdentityApplyHubLeafProvider implements HubLeafProvider<IdentityStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.OPEN;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<IdentityStore> o) {
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
    public ObservableValue<String> getName(DataStoreEntryRef<IdentityStore> store) {
        return AppI18n.observable("applyIdentityToHost");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<IdentityStore> store) {
        return new LabelGraphic.IconGraphic("mdi2e-export");
    }

    @Override
    public Class<?> getApplicableClass() {
        return IdentityStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<IdentityStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "applyIdentity";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<IdentityStore> {

        @Override
        public void executeImpl() {
            if (ref.getStore().getSshIdentity() != null && !(ref.getStore().getSshIdentity() instanceof NoIdentityStrategy) && ref.getStore().getSshIdentity().getPublicKey() == null) {
                AppDialog.confirm("identityApplyMissingPublicKey");
                StoreCreationDialog.showEdit(ref.get());
                return;
            }

            IdentityApplyDialog.show(ref.getStore());
        }
    }
}
