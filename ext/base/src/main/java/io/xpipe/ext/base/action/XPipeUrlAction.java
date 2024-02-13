package io.xpipe.ext.base.action;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.comp.store.StoreCreationComp;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.store.LaunchableStore;
import io.xpipe.core.util.InPlaceSecretValue;
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
            actionProvider.getDataStoreCallSite().createAction(entry.ref()).execute();
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

                TerminalLauncher.open(storeName, command);
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
            StoreCreationComp.showEdit(entry);
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
                switch (args.get(0)) {
                    case "addStore" -> {
                        var storeString = InPlaceSecretValue.builder()
                                .encryptedValue(args.get(1))
                                .build();
                        var store = JacksonMapper.parse(storeString.getSecretValue(), DataStore.class);
                        return new AddStoreAction(store);
                    }
                    case "launch" -> {
                        var entry = DataStorage.get()
                                .getStoreEntryIfPresent(UUID.fromString(args.get(1)))
                                .orElseThrow();
                        if (!entry.getValidity().isUsable()) {
                            return null;
                        }
                        return new LaunchAction(entry);
                    }
                    case "action" -> {
                        var id = args.get(1);
                        ActionProvider provider = ActionProvider.ALL.stream().filter(actionProvider -> {
                            return actionProvider.getDataStoreCallSite() != null && id.equals(actionProvider.getId());
                        }).findFirst().orElseThrow();
                        var entry = DataStorage.get()
                                .getStoreEntryIfPresent(UUID.fromString(args.get(2)))
                                .orElseThrow();
                        if (!entry.getValidity().isUsable()) {
                            return null;
                        }
                        return new CallAction(provider, entry);
                    }
                    default -> {
                        return null;
                    }
                }
            }
        };
    }
}
