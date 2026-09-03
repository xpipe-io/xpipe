package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.FilePath;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class GzipActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "gzip";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final FilePath target;

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            var b = CommandBuilder.of().add("gzip", "--keep", "--force", "--stdout");
            for (BrowserEntry entry : getEntries()) {
                b.addFile(entry.getRawFileEntry().getPath());
            }
            b.add(">").addFile(target);
            sc.command(b).execute();
            model.refreshSync();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
