package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.ext.FileKind;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ZipActionProvider implements BrowserActionProvider {

    @Override
    public String getId() {
        return "zip";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @NonNull
        private final FilePath target;

        private final boolean directoryContentOnly;

        @Override
        public void executeImpl() throws Exception {
            try {
                var sc = model.getFileSystem().getShell().orElseThrow();
                if (sc.getOsType() == OsType.WINDOWS) {
                    var base = model.getCurrentDirectory().getPath();
                    var command = CommandBuilder.of()
                            .add("Compress-Archive", "-Force", "-DestinationPath")
                            .addFile(target)
                            .add("-Path");
                    for (int i = 0; i < getEntries().size(); i++) {
                        var rel =
                                getEntries().get(i).getRawFileEntry().getPath().relativize(base);
                        if (getEntries().get(i).getRawFileEntry().getKind() == FileKind.DIRECTORY
                                && directoryContentOnly) {
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
                    var command = CommandBuilder.of().add("zip", "-q", "-y", "-r", "-");
                    for (BrowserEntry entry : getEntries()) {
                        var base = target.getParent();
                        var rel = entry.getRawFileEntry()
                                .getPath()
                                .relativize(base)
                                .toUnix();
                        if (entry.getRawFileEntry().getKind() == FileKind.DIRECTORY && directoryContentOnly) {
                            command.add(".");
                        } else {
                            command.addFile(rel);
                        }
                    }
                    command.add(">").addFile(target);

                    if (directoryContentOnly) {
                        sc.command(command)
                                .withWorkingDirectory(getEntries()
                                        .getFirst()
                                        .getRawFileEntry()
                                        .getPath())
                                .execute();
                    } else {
                        sc.command(command).execute();
                    }
                }
            } finally {
                model.refreshSync();
            }
        }

        @Override
        public boolean isMutation() {
            return true;
        }
    }
}
