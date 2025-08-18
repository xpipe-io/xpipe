package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class UntarActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        private final boolean gz;
        private final boolean toDirectory;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            ShellControl sc = model.getFileSystem().getShell().orElseThrow();
            for (BrowserEntry entry : getEntries()) {
                var target = getTarget(entry.getRawFileEntry().getPath());
                var c = CommandBuilder.of().add("tar");
                if (toDirectory) {
                    c.add("-C").addFile(target);
                }
                c.add("-x").addIf(gz, "-z").add("-f");
                c.addFile(entry.getRawFileEntry().getPath());
                if (toDirectory) {
                    model.getFileSystem().mkdirs(target);
                }
                sc.command(c)
                        .withWorkingDirectory(model.getCurrentDirectory().getPath())
                        .execute();
            }
            model.refreshSync();
        }

        private FilePath getTarget(FilePath name) {
            return FilePath.of(name.toString()
                    .replaceAll("\\.tar$", "")
                    .replaceAll("\\.tar.gz$", "")
                    .replaceAll("\\.tgz$", ""));
        }
    }

    @Override
    public String getId() {
        return "untar";
    }
}
