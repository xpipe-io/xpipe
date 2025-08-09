package io.xpipe.ext.base.script;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLaunch;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunTerminalScriptActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "runTerminalStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ShellStore> {

        DataStoreEntryRef<SimpleScriptStore> scriptStore;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var sc = ref.getStore().getOrStartSession();
            var script = scriptStore.getStore().assembleScriptChain(sc);
            TerminalLaunch.builder().entry(ref.get()).title(scriptStore.get().getName()).command(sc.command(script)).launch();
        }
    }
}
