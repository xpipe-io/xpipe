package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ProcessOutputException;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.atomic.AtomicReference;

public class RunCommandInBackgroundActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runFileInBackground";
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
            var cmd = CommandBuilder.of().addFile(command);
            for (BrowserEntry entry : getEntries()) {
                cmd.addFile(entry.getRawFileEntry().getPath());
            }

            AtomicReference<String> out = new AtomicReference<>();
            AtomicReference<String> err = new AtomicReference<>();
            long exitCode;
            try (var command = model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .command(cmd)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .start()) {
                var r = command.readStdoutAndStderr();
                out.set(r[0]);
                err.set(r[1]);
                exitCode = command.getExitCode();
            }

            // Only throw actual error output
            if (exitCode != 0) {
                throw ErrorEvent.expected(ProcessOutputException.of(exitCode, out.get(), err.get()));
            }
        }
    }
}
