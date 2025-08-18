package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.core.FileKind;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class OpenFileDefaultActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() {
            for (var entry : getEntries()) {
                BrowserFileOpener.openInDefaultApplication(model, entry.getRawFileEntry());
            }
        }
    }

    @Override
    public String getId() {
        return "openFileDefault";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileList().getEditing().getValue() == null
                && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }
}
