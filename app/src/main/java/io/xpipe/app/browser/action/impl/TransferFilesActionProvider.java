package io.xpipe.app.browser.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.StoreContextAction;
import io.xpipe.app.browser.file.BrowserFileTransferOperation;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystemStore;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransferFilesActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "transferFiles";
    }

    @Override
    public boolean isMutation() {
        return true;
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends AbstractAction implements StoreContextAction {

        @NonNull
        DataStoreEntryRef<FileSystemStore> target;

        @NonNull
        BrowserFileTransferOperation operation;

        @Override
        public void executeImpl() throws Exception {
            operation.execute();
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<String, String>();
            map.put("action", getDisplayName());
            map.put("sources", operation.getFiles().stream().map(fileEntry -> fileEntry.getName()).collect(Collectors.joining("\n")));
            map.put("target", DataStorage.get().getStoreEntryDisplayName(target.get()));
            map.put("targetDirectory", operation.getTarget().getPath().toString());
            return map;
        }

        @Override
        public List<DataStoreEntry> getStoreEntryContext() {
            return List.of(target.get());
        }
    }
}
