package io.xpipe.ext.base.actions;

import io.xpipe.app.comp.source.store.GuiDsStoreCreator;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DefaultSecretValue;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import lombok.Value;

import java.util.List;
import java.util.UUID;

public class AddStoreAction implements ActionProvider {
    @Value
    static class Action implements ActionProvider.Action {

        DataStore store;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() {
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
                var storeString =
                        DefaultSecretValue.builder().encryptedValue(args.get(0)).build();
                var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
                return new Action(store);
            }
        };
    }
}
