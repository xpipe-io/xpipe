package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ZipActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final FilePath target;

        private final boolean directoryContentOnly;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            if (sc.getOsType() == OsType.WINDOWS) {
                var base = model.getCurrentDirectory().getPath();
                var command = CommandBuilder.of()
                        .add("Compress-Archive", "-Force", "-DestinationPath")
                        .addFile(target)
                        .add("-Path");
                for (int i = 0; i < getEntries().size(); i++) {
                    var rel = getEntries().get(i).getRawFileEntry().getPath().relativize(base);
                    if (getEntries().get(i).getRawFileEntry().getKind() == FileKind.DIRECTORY && directoryContentOnly) {
                        command.addQuoted(rel.toDirectory().toWindows() + "*");
                    } else {
                        command.addFile(rel.toWindows());
                    }
                    if (i != getEntries().size() - 1) {
                        command.add(",");
                    }
                }

                if (ShellDialects.isPowershell(sc)) {
                    sc.command(command).withWorkingDirectory(base).execute();
                } else {
                    try (var sub = sc.subShell(ShellDialects.POWERSHELL)) {
                        sub.command(command).withWorkingDirectory(base).execute();
                    }
                }
            } else {
                var command = CommandBuilder.of().add("zip", "-r", "-");
                for (BrowserEntry entry : getEntries()) {
                    var base = target.getParent();
                    var rel = entry.getRawFileEntry().getPath().relativize(base).toUnix();
                    if (entry.getRawFileEntry().getKind() == FileKind.DIRECTORY && directoryContentOnly) {
                        command.add(".");
                    } else {
                        command.addFile(rel);
                    }
                }
                command.add(">").addFile(target);

                if (directoryContentOnly) {
                        sc.command(command)
                                .withWorkingDirectory(
                                        getEntries().getFirst().getRawFileEntry().getPath())
                                .execute();
                } else {
                    sc.command(command).execute();
                }
            }
            model.refreshSync();
        }
    }

    @Override
    public String getId() {
        return "zip";
    }
}
