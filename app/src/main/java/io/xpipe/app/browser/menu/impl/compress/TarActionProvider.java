package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.store.FilePath;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class TarActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final FilePath target;

        private final boolean directoryContentOnly;

        private final boolean gz;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            var tar = CommandBuilder.of()
                    .add("tar", "-c")
                    .addIf(gz, "-z")
                    .add("-f")
                    .addFile(target);
            var base = model.getCurrentDirectory().getPath();

            if (directoryContentOnly) {
                var dir = getEntries().getFirst().getRawFileEntry().getPath();
                // Fix for bsd find, remove /
                var command = CommandBuilder.of()
                        .add("find")
                        .addFile(dir.removeTrailingSlash().toUnix())
                        .add("|", "sed")
                        .addLiteral("s,^" + dir.toDirectory().toUnix() + "*,,")
                        .add("|");
                command.add(tar).add("-C").addFile(dir.toDirectory().toUnix()).add("-T", "-");
                sc.command(command).execute();
            } else {
                var command = CommandBuilder.of().add(tar);
                for (BrowserEntry entry : getEntries()) {
                    var rel = entry.getRawFileEntry().getPath().relativize(base);
                    command.addFile(rel);
                }
                sc.command(command).execute();
            }
            model.refreshSync();
        }
    }

    @Override
    public String getId() {
        return "tar";
    }
}
