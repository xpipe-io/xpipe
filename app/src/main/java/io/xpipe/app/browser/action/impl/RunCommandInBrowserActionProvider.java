package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.core.process.CommandBuilder;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunCommandInBrowserActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runCommandInBrowser";
    }

    @Override
    public boolean isMutation() {
        return true;
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String command;

        @Override
        public void executeImpl() throws Exception {
            var builder = CommandBuilder.of().addFile(command);
            for (BrowserEntry entry : getEntries()) {
                builder.addFile(entry.getRawFileEntry().getPath());
            }

            var cmd = model.getFileSystem().getShell().orElseThrow().command(builder);
            CommandDialog.runAsyncAndShow(cmd);
        }
    }
}
