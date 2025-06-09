package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.*;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class DeleteActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            var toDelete =
                    getEntries().stream().map(entry -> entry.getRawFileEntry()).toList();
            BrowserFileSystemHelper.delete(toDelete);
            model.refreshSync();
        }
    }

    @Override
    public String getId() {
        return "deleteFile";
    }

    @Override
    public boolean isMutation() {
        return true;
    }
}
