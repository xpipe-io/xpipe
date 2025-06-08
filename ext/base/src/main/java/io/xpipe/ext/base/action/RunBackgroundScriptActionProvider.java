package io.xpipe.ext.base.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.ext.base.script.SimpleScriptStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


public class RunBackgroundScriptActionProvider implements ActionProvider {

        @Override
    public String getId() {
        return "runBackgroundScript";
    }
@Jacksonized
@SuperBuilder
    public static class Action extends StoreAction<ShellStore> {

        private final DataStoreEntryRef<SimpleScriptStore> scriptStore;

        @Override
        public void executeImpl() throws Exception {
            var sc = ref.getStore().getOrStartSession();
            var script = scriptStore.getStore().assembleScriptChain(sc);
            sc.command(script).execute();
        }
    }

    @Override
    public boolean isMutation() {
        return true;
    }
}
