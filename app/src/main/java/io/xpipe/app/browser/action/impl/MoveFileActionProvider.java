package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.core.store.FilePath;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class MoveFileActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        FilePath target;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            model.getFileSystem().move(getEntries().getFirst().getRawFileEntry().getPath(), target);
            model.refreshSync();
        }
    }

    @Override
    public String getId() {
        return "moveFile";
    }
}
