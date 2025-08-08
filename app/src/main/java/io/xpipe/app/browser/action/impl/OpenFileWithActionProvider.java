package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.core.FileKind;
import io.xpipe.core.OsType;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class OpenFileWithActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() {
            for (var entry : getEntries()) {
                BrowserFileOpener.openWithAnyApplication(model, entry.getRawFileEntry());
            }
        }
    }

    @Override
    public String getId() {
        return "openFileWith";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return OsType.getLocal() == OsType.WINDOWS
                && entries.size() == 1
                && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }
}
