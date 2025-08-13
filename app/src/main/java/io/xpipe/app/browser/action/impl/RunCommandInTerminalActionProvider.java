package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunCommandInTerminalActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runCommandInTerminal";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        String title;

        @NonNull
        String command;

        @Override
        public void executeImpl() throws Exception {
            var wd = files.getFirst();
            model.openTerminalSync(
                    title,
                    wd,
                    model.getFileSystem()
                            .getShell()
                            .orElseThrow()
                            .command(command)
                            .withWorkingDirectory(wd),
                    true);
        }

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<>(super.toDisplayMap());
            map.remove("Title");
            map.remove("Files");
            map.put("Working Directory", files.getFirst().toString());
            return map;
        }
    }
}
