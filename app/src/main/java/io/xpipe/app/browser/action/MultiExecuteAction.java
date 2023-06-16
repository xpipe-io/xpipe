package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellControl;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

public abstract class MultiExecuteAction implements BranchAction {

    protected String filesArgument(List<BrowserEntry> entries) {
        return entries.size() == 1 ? entries.get(0).getOptionallyQuotedFileName() : "(" + entries.size() + ")";
    }

    protected abstract String createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry);

    @Override
    public List<LeafAction> getBranchingActions() {
        return List.of(
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var cmd = pc.command(createCommand(pc, model, entry))
                                                .withWorkingDirectory(model.getCurrentDirectory()
                                                        .getPath())
                                                .prepareTerminalOpen(FileNames.getFileName(
                                                        entry.getRawFileEntry().getPath()));
                                        TerminalHelper.open(
                                                FilenameUtils.getBaseName(
                                                        entry.getRawFileEntry().getPath()),
                                                cmd);
                                    }
                                },
                                false);
                    }

                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return "in " + AppPrefs.get().terminalType().getValue().toTranslatedString();
                    }
                },
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var cmd = ScriptHelper.createDetachCommand(pc, createCommand(pc, model, entry));
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
