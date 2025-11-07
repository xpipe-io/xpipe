package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.DesktopHelper;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class BrowseInNativeManagerActionProvider implements BrowserActionProvider {

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return false;
        }

        return model.getFileSystem()
                .getShell()
                .orElseThrow()
                .getLocalSystemAccess()
                .supportsFileSystemAccess();
    }

    @Override
    public String getId() {
        return "browseInNativeFileManager";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            ShellControl sc = model.getFileSystem().getShell().orElseThrow();
            for (BrowserEntry entry : getEntries()) {
                var e = entry.getRawFileEntry().getPath();
                var localFile = sc.getLocalSystemAccess().translateToLocalSystemPath(e);
                DesktopHelper.browseFileInDirectory(localFile.asLocalPath());
            }
        }
    }
}
