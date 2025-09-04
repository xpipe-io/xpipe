package io.xpipe.app.browser.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserFileOutput;

import io.xpipe.app.storage.DataStorage;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApplyFileEditActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "applyFileEdit";
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
            output.beforeTransfer();
            try (var out = output.open()) {
                input.transferTo(out);
            }
            output.onFinish();
        }

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<String, String>();
            map.put("Action", getDisplayName());

            var system = output.target();
            if (system.isPresent()) {
                map.put("System", DataStorage.get().getStoreEntryDisplayName(system.get()));
            }

            map.put("File", target);

            return map;
        }
    }
}
