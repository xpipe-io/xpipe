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

public class UnzipActionProvider implements BrowserActionProvider {

    public static FilePath getTarget(FilePath name) {
        return FilePath.of(name.toString().replaceAll("\\.zip$", ""));
    }

    @Override
    public String getId() {
        return "unzip";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        private final boolean toDirectory;

        @Override
        public void executeImpl() throws Exception {
            var sc = model.getFileSystem().getShell().orElseThrow();
            if (sc.getOsType() == OsType.WINDOWS) {
                sc.enforceDialect(ShellDialects.POWERSHELL, p -> {
                    for (BrowserEntry entry : getEntries()) {
                        runPowershellCommand(p, model, entry);
                    }
                    return null;
                });
            } else {
                for (BrowserEntry entry : getEntries()) {
                    var command = CommandBuilder.of()
                            .add("unzip", "-o")
                            .addFile(entry.getRawFileEntry().getPath());
                    if (toDirectory) {
                        command.add("-d")
                                .addFile(getTarget(entry.getRawFileEntry().getPath()));
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

        @Override
        public boolean isMutation() {
            return true;
        }

        private void runPowershellCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry)
                throws Exception {
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
}
