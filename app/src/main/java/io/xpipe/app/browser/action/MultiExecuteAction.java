package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.ShellControl;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

public abstract class MultiExecuteAction implements BranchAction {

    protected abstract String createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry);

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        TerminalLauncher.open(
                                                model.getEntry().getEntry(),
                                                FilenameUtils.getBaseName(
                                                        entry.getRawFileEntry().getPath()),
                                                model.getCurrentDirectory() != null
                                                        ? model.getCurrentDirectory()
                                                                .getPath()
                                                        : null,
                                                pc.command(createCommand(pc, model, entry)));
                                    }
                                },
                                false);
                    }

                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var t = AppPrefs.get().terminalType().getValue();
                        return "in " + (t != null ? t.toTranslatedString() : "?");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return AppPrefs.get().terminalType().getValue() != null;
                    }
                },
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var cmd = ApplicationHelper.createDetachCommand(
                                                pc, createCommand(pc, model, entry));
                                        pc.command(cmd)
                                                .withWorkingDirectory(model.getCurrentDirectory()
                                                        .getPath())
                                                .execute();
                                    }
                                },
                                false);
                    }

                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return "in background";
                    }
                },
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        pc.command(createCommand(pc, model, entry))
                                                .withWorkingDirectory(model.getCurrentDirectory()
                                                        .getPath())
                                                .execute();
                                    }
                                },
                                false);
                    }

                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return "wait for completion";
                    }
                });
    }
}
