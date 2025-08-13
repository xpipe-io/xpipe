package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.CommandDialog;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunCommandInBrowserActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runCommandInBrowser";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String command;

        @Override
        public void executeImpl() {
            var cmd = model.getFileSystem().getShell().orElseThrow().command(command).withWorkingDirectory(files.getFirst());
            CommandDialog.runAndShow(cmd);
            model.refreshSync();
        }

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<>(super.toDisplayMap());
            map.remove("Files");
            map.put("Working Directory", files.getFirst().toString());
            return map;
        }
    }
}
