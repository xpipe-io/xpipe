package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.core.OsType;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ChgrpActionProvider implements BrowserActionProvider {

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().supportsChgrp();
    }

    @Override
    public String getId() {
        return "chgrp";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final String group;

        private final boolean recursive;

        @Override
        public void executeImpl() throws Exception {
            for (BrowserEntry entry : getEntries()) {
                model.getFileSystem().chgrp(entry.getRawFileEntry().getPath(), group, recursive);
            }
            model.refreshBrowserEntriesSync(getEntries());
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
