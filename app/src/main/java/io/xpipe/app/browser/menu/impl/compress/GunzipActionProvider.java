package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class GunzipActionProvider implements BrowserActionProvider {

    public static FilePath getTarget(FilePath name) {
        return FilePath.of(name.toString().replaceAll("\\.gz$", "").replaceAll("\\.gzip$", ""));
    }

    @Override
    public String getId() {
        return "gunzip";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            var b = CommandBuilder.of().add("gunzip", "--keep", "--force");
            for (BrowserEntry entry : getEntries()) {
                b.addFile(entry.getRawFileEntry().getPath());
            }
            sc.command(b).execute();
            model.refreshSync();
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
