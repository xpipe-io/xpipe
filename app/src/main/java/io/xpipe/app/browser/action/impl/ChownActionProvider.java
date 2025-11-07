package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ChownActionProvider implements BrowserActionProvider {

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().supportsChown();
    }

    @Override
    public String getId() {
        return "chown";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final String owner;

        private final boolean recursive;

        @Override
        public void executeImpl() throws Exception {
            for (BrowserEntry entry : getEntries()) {
                model.getFileSystem().chown(entry.getRawFileEntry().getPath(), owner, recursive);
            }
            model.refreshBrowserEntriesSync(getEntries());
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
