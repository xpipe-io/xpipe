package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RunCommandInTerminalActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runCommandInTerminal";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String title;

        @NonNull
        String command;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var cmd = CommandBuilder.of().add(command);
            for (BrowserEntry entry : getEntries()) {
                cmd.addFile(entry.getRawFileEntry().getPath());
            }

            model.openTerminalSync(
                    title,
                    model.getCurrentDirectory() != null
                            ? model.getCurrentDirectory().getPath()
                            : null,
                    model.getFileSystem().getShell().orElseThrow().command(cmd),
                    true);
        }
    }
}
