package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class NewLinkActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String name;

        @NonNull
        FilePath target;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            for (BrowserEntry entry : getEntries()) {
                if (entry.getRawFileEntry().getKind() != FileKind.DIRECTORY) {
                    continue;
                }

                var file = entry.getRawFileEntry().getPath().join(name);
                model.getFileSystem().symbolicLink(file, target);
            }
            model.refreshSync();
        }
    }

    @Override
    public String getId() {
        return "newLink";
    }
}
