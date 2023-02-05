package io.xpipe.ext.base.actions;

import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.util.ActionProvider;
import lombok.Value;

import java.util.List;
import java.util.UUID;

public class AddStoreAction implements ActionProvider {
    @Value
    static class Action implements ActionProvider.Action {

        DataStore store;

        @Override
        public boolean requiresPlatform() {
            return true;
        }
        @Override
        public void execute() throws Exception {
            if (store == null) {
                return;
            }

            var entry = DataStoreEntry.createNew(UUID.randomUUID(), "", store);
            GuiDsStoreCreator.showEdit(entry);
        }
    }

    @Override
    public LauncherCallSite getLauncherCallSite() {
        return new LauncherCallSite() {
            @Override
            public String getId() {
                return "addStore";
            }

            @Override
            public Action createAction(List<String> args) throws Exception {
                var storeString = SecretValue.ofSecret(args.get(1));
                var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
                return new Action(store);
            }
        };
    }
}
