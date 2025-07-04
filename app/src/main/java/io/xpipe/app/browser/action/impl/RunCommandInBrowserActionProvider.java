package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.CommandDialog;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunCommandInBrowserActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runCommandInBrowser";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String command;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var builder = CommandBuilder.of().add(command);
            for (BrowserEntry entry : getEntries()) {
                builder.addFile(entry.getRawFileEntry().getPath());
            }

            var cmd = model.getFileSystem().getShell().orElseThrow().command(builder);
            CommandDialog.runAndShow(cmd);
            model.refreshBrowserEntriesSync(getEntries());
        }
    }
}
