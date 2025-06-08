package io.xpipe.ext.base.script;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.core.process.CommandControl;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;

public class RunHubBatchScriptActionProvider implements ActionProvider {

        @Override
    public String getId() {
        return "runHubBatchScript";
    }
@Jacksonized
@SuperBuilder
    public static class Action extends MultiStoreAction<ShellStore> {

        private final DataStoreEntryRef<SimpleScriptStore> scriptStore;

        @Override
        public void executeImpl() throws Exception {
            var map = new LinkedHashMap<String, CommandControl>();
            for (DataStoreEntryRef<ShellStore> ref : refs) {
                var sc = ref.getStore().getOrStartSession();
                var script = scriptStore.getStore().assembleScriptChain(sc);
                var cmd = sc.command(script);
                map.put(ref.get().getName(), cmd);
            }
            CommandDialog.runAsyncAndShow(map);
        }
    }

    @Override
    public boolean isMutation() {
        return true;
    }
}
