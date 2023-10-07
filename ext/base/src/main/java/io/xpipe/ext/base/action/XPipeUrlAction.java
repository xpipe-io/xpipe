package io.xpipe.ext.base.action;

import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.comp.store.GuiDsStoreCreator;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.core.store.LaunchableStore;
import io.xpipe.core.util.DefaultSecretValue;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import lombok.Value;

import java.util.List;
import java.util.UUID;

public class XPipeUrlAction implements ActionProvider {

    @Value
    static class CallAction implements ActionProvider.Action {

        ActionProvider actionProvider;
        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            actionProvider.getDataStoreCallSite().createAction(entry.getStore().asNeeded()).execute();
        }
    }

    @Value
    static class LaunchAction implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            var storeName = entry.getName();
            if (entry.getStore() instanceof LaunchableStore s) {
                var command = s.prepareLaunchCommand();
                if (command == null) {
                    return;
                }

                TerminalHelper.open(storeName, command);
            }
        }
    }

    @Value
    static class AddStoreAction implements ActionProvider.Action {

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

            var entry = DataStoreEntry.createNew(UUID.randomUUID(), StoreViewState.get().getActiveCategory().getValue().getCategory().getUuid(), "", store);
            GuiDsStoreCreator.showEdit(entry);
        }
    }

    @Override
    public LauncherCallSite getLauncherCallSite() {
        return new XPipeLauncherCallSite() {

            @Override
            public String getId() {
                return "xpipe";
            }

            @Override
            public Action createAction(List<String> args) throws Exception {
                if (args.get(0).equals("addStore")) {
                    var storeString = DefaultSecretValue.builder()
                            .encryptedValue(args.get(1))
                            .build();
                    var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
                    return new AddStoreAction(store);
                } else if (args.get(0).equals("launch")) {
                    var entry = DataStorage.get()
                            .getStoreEntryIfPresent(UUID.fromString(args.get(1)))
                            .orElseThrow();
                    return new LaunchAction(entry);
                } else if (args.get(0).equals("action")) {
                    var id = args.get(1);
                    ActionProvider provider = ActionProvider.ALL.stream().filter(actionProvider -> {
                        return actionProvider.getDataStoreCallSite() != null && id.equals(actionProvider.getId());
                    }).findFirst().orElseThrow();
                    var entry = DataStorage.get()
                            .getStoreEntryIfPresent(UUID.fromString(args.get(2)))
                            .orElseThrow();
                    return new CallAction(provider, entry);
                } else {
                    return null;
                }
            }
        };
    }
}
