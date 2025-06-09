package io.xpipe.ext.base.script;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandDialog;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunHubScriptActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "runHubScript";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ShellStore> {

        DataStoreEntryRef<SimpleScriptStore> scriptStore;

        @Override
        public void executeImpl() throws Exception {
            var sc = ref.getStore().getOrStartSession();
            var script = scriptStore.getStore().assembleScriptChain(sc);
            var cmd = sc.command(script);
            CommandDialog.runAsyncAndShow(cmd);
        }
    }

    @Override
    public boolean isMutation() {
        return true;
    }
}
