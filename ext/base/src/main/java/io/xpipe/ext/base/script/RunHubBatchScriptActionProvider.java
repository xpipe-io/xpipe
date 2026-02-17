package io.xpipe.ext.base.script;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandDialog;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;

public class RunHubBatchScriptActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "runHubBatchScript";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends MultiStoreAction<ShellStore> {

        private final DataStoreEntryRef<ScriptStore> scriptStore;

        @Override
        public void executeImpl() throws Exception {
            var list = new ArrayList<CommandDialog.CommandEntry>();
            for (DataStoreEntryRef<ShellStore> ref : refs) {
                var sc = ref.getStore().getOrStartSession();
                var script = scriptStore.getStore().assembleScriptChain(sc, false);
                if (script == null) {
                    continue;
                }

                var cmd = sc.command(script);
                list.add(new CommandDialog.CommandEntry(ref.get().getName(), cmd));
            }
            CommandDialog.runMultipleAndShow(list);
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
