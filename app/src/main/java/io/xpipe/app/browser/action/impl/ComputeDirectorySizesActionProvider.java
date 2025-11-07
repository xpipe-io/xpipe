package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.ext.FileKind;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ComputeDirectorySizesActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "computeDirectorySizes";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().supportsDirectorySizes();
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            var entries = getEntries();
            if (entries.size() == 1 && entries.getFirst().getRawFileEntry().equals(model.getCurrentDirectory())) {
                entries = model.getFileList().getAll().getValue();
            }

            for (BrowserEntry be : entries) {
                if (be.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY) {
                    continue;
                }

                var size = model.getFileSystem()
                        .getDirectorySize(be.getRawFileEntry().resolved().getPath());
                var fileEntry = be.getRawFileEntry();
                fileEntry.resolved().setSize("" + size);
                model.getFileList().updateEntry(be.getRawFileEntry().getPath(), fileEntry);
            }
        }
    }
}
