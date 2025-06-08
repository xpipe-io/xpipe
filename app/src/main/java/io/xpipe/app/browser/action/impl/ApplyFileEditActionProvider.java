package io.xpipe.app.browser.action.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserFileOutput;
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
