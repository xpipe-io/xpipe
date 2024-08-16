package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class MultiExecuteSelectionAction implements BranchAction {

    protected abstract CommandBuilder createCommand(
            ShellControl sc, OpenFileSystemModel model, List<BrowserEntry> entries);

    protected abstract String getTerminalTitle();

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new LeafAction() {

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var cmd = pc.command(createCommand(pc, model, entries));
                                    TerminalLauncher.open(
                                            model.getEntry().getEntry(),
                                            getTerminalTitle(),
                                            model.getCurrentDirectory() != null
                                                    ? model.getCurrentDirectory()
                                                            .getPath()
                                                    : null,
                                            cmd);
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var t = AppPrefs.get().terminalType().getValue();
                        return AppI18n.observable(
                                "executeInTerminal",
                                t != null ? t.toTranslatedString().getValue() : "?");
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
                                    var cmd = createCommand(pc, model, entries);
                                    pc.command(cmd)
                                            .withWorkingDirectory(
                                                    model.getCurrentDirectory().getPath())
                                            .execute();
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("executeInBackground");
                    }
                });
    }
}
