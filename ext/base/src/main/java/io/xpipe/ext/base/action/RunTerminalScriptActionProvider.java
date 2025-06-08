package io.xpipe.ext.base.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.ext.base.script.SimpleScriptStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunTerminalScriptActionProvider implements ActionProvider {

    @Override
    public boolean isMutation() {
        return true;
    }

        @Override
    public String getId() {
        return "runTerminalStore";
    }
@Jacksonized
@SuperBuilder
    public static class Action extends StoreAction<ShellStore> {

        DataStoreEntryRef<SimpleScriptStore> scriptStore;

        @Override
        public void executeImpl() throws Exception {
            var sc = ref.getStore().getOrStartSession();
            var script = scriptStore.getStore().assembleScriptChain(sc);
            TerminalLauncher.open(ref.get(), scriptStore.get().getName() + " - " + ref.get().getName(), null,
                    sc.command(script));
        }
    }
}
