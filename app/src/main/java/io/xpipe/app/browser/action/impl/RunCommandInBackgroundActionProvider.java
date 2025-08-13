package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ProcessOutputException;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RunCommandInBackgroundActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "runFileInBackground";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        String command;

        @Override
        public void executeImpl() throws Exception {
            AtomicReference<String> out = new AtomicReference<>();
            AtomicReference<String> err = new AtomicReference<>();
            long exitCode;
            try (var cc = model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .command(command)
                    .withWorkingDirectory(files.getFirst())
                    .start()) {
                var r = cc.readStdoutAndStderr();
                out.set(r[0]);
                err.set(r[1]);
                exitCode = cc.getExitCode();
            }

            model.refreshSync();

            // Only throw actual error output
            if (exitCode != 0) {
                throw ErrorEventFactory.expected(ProcessOutputException.of(exitCode, out.get(), err.get()));
            }
        }

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<>(super.toDisplayMap());
            map.remove("Title");
            map.remove("Files");
            map.put("Working Directory", files.getFirst().toString());
            return map;
        }
    }
}
