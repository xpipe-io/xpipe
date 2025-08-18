package io.xpipe.app.browser.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.StoreContextAction;
import io.xpipe.app.browser.file.BrowserFileTransferOperation;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransferFilesActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "transferFiles";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends AbstractAction implements StoreContextAction {

        @NonNull
        DataStoreEntryRef<FileSystemStore> target;

        @NonNull
        BrowserFileTransferOperation operation;

        boolean download;

        @Override
        public boolean isMutation() {
            return !download;
        }

        @Override
        public boolean forceConfirmation() {
            return operation.isMove();
        }

        @Override
        public void executeImpl() throws Exception {
            operation.execute();
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var name = operation.isMove() ? "Move files" : getDisplayName();
            var map = new LinkedHashMap<String, String>();
            map.put("Action", name);
            map.put(
                    "Sources",
                    operation.getFiles().stream()
                            .map(fileEntry -> fileEntry.getName())
                            .collect(Collectors.joining("\n")));
            map.put("Target system", DataStorage.get().getStoreEntryDisplayName(target.get()));
            map.put("Target directory", operation.getTarget().getPath().toString());
            return map;
        }

        @Override
        public List<DataStoreEntry> getStoreEntryContext() {
            return List.of(target.get());
        }
    }
}
