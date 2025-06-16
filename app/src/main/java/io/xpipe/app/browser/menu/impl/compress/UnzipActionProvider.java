package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class UnzipActionProvider implements BrowserActionProvider {

    public static FilePath getTarget(FilePath name) {
        return FilePath.of(name.toString().replaceAll("\\.zip$", ""));
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        private final boolean toDirectory;

        @Override
        public boolean isMutation() {
            return true;
        }

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            if (sc.getOsType() == OsType.WINDOWS) {
                if (ShellDialects.isPowershell(sc)) {
                    for (BrowserEntry entry : getEntries()) {
                        runPowershellCommand(sc, model, entry);
                    }
                } else {
                    try (var sub = sc.subShell(ShellDialects.POWERSHELL)) {
                        for (BrowserEntry entry : getEntries()) {
                            runPowershellCommand(sub, model, entry);
                        }
                    }
                }
            } else {
                for (BrowserEntry entry : getEntries()) {
                    var command = CommandBuilder.of()
                            .add("unzip", "-o")
                            .addFile(entry.getRawFileEntry().getPath());
                    if (toDirectory) {
                        command.add("-d").addFile(getTarget(entry.getRawFileEntry().getPath()));
                    }
                    try (var cc = sc.command(command)
                            .withWorkingDirectory(model.getCurrentDirectory().getPath())
                            .start()) {
                        cc.discardOrThrow();
                    }
                }
            }
            model.refreshSync();
        }

        private void runPowershellCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) throws Exception {
            var command = CommandBuilder.of().add("Expand-Archive", "-Force");
            if (toDirectory) {
                var target = getTarget(entry.getRawFileEntry().getPath());
                command.add("-DestinationPath").addFile(target);
            }
            command.add("-Path").addFile(entry.getRawFileEntry().getPath());
            sc.command(command)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .execute();
        }
    }

    @Override
    public String getId() {
        return "unzip";
    }
}
