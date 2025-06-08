package io.xpipe.app.browser.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserFileOutput;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FilePath;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ApplyFileEditActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "applyFileEdit";
    }

    @Override
    public boolean isMutation() {
        return true;
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends AbstractAction {

        @NonNull
        String target;
        @NonNull
        InputStream input;
        @NonNull
        BrowserFileOutput output;

        @Override
        public void executeImpl() throws Exception {
            try (var out = output.open()) {
                input.transferTo(out);
            }
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<String, String>();
            map.put("action", getDisplayName());
            map.put("target", target);
            return map;
        }
    }
}
