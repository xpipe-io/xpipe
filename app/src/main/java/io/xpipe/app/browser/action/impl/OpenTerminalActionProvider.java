package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Collections;
import java.util.List;

public class OpenTerminalActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            var entries = getEntries();
            var dirs = entries.size() > 0
                    ? entries.stream()
                            .map(browserEntry -> browserEntry.getRawFileEntry().getPath())
                            .toList()
                    : model.getCurrentDirectory() != null
                            ? List.of(model.getCurrentDirectory().getPath())
                            : Collections.singletonList((FilePath) null);
            for (var dir : dirs) {
                var name = (dir != null ? dir + " - " : "") + model.getName().getValue();
                model.openTerminalSync(
                        name, dir, model.getFileSystem().getShell().orElseThrow(), dirs.size() == 1);
            }
        }
    }

    @Override
    public String getId() {
        return "openTerminalInDirectory";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var t = AppPrefs.get().terminalType().getValue();
        return t != null;
    }
}
