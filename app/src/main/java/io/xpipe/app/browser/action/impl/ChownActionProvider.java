package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ChownActionProvider implements BrowserActionProvider {

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var os = model.getFileSystem().getShell().orElseThrow().getOsType();
        return os != OsType.WINDOWS && os != OsType.MACOS;
    }

    @Override
    public String getId() {
        return "chown";
    }

    @Override
    public boolean isMutation() {
        return true;
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final String owner;
        private final boolean recursive;

        @Override
        public void executeImpl() throws Exception {
            model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("chown")
                            .addIf(recursive, "-R")
                            .addLiteral(owner)
                            .addFiles(getEntries().stream()
                                    .map(browserEntry -> browserEntry
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString())
                                    .toList()));
        }
    }
}
