package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellControl;

import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class MultiExecuteSelectionAction implements BrowserBranchAction {

    protected abstract CommandBuilder createCommand(
            ShellControl sc, BrowserFileSystemTabModel model, List<BrowserEntry> entries);

    protected abstract String getTerminalTitle();

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var c = createCommand(pc, model, entries);
                                    if (c == null) {
                                        return;
                                    }

                                    var cmd = pc.command(c);
                                    model.openTerminalAsync(
                                            getTerminalTitle(),
                                            model.getCurrentDirectory() != null
                                                    ? model.getCurrentDirectory()
                                                            .getPath()
                                                    : null,
                                            cmd,
                                            true);
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var t = AppPrefs.get().terminalType().getValue();
                        return AppI18n.observable(
                                "executeInTerminal",
                                t != null ? t.toTranslatedString().getValue() : "?");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppPrefs.get().terminalType().getValue() != null;
                    }
                },
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var c = createCommand(pc, model, entries);
                                    if (c == null) {
                                        return;
                                    }

                                    var cmd = pc.command(c);
                                    CommandDialog.runAsyncAndShow(cmd);
                                },
                                true);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runInFileBrowser");
                    }
                },
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    var cmd = createCommand(pc, model, entries);
                                    AtomicReference<String> out = new AtomicReference<>();
                                    AtomicReference<String> err = new AtomicReference<>();
                                    long exitCode;
                                    try (var command = pc.command(cmd)
                                            .withWorkingDirectory(
                                                    model.getCurrentDirectory().getPath())
                                            .start()) {
                                        var r = command.readStdoutAndStderr();
                                        out.set(r[0]);
                                        err.set(r[1]);
                                        exitCode = command.getExitCode();
                                    }
                                    // Only throw actual error output
                                    if (exitCode != 0) {
                                        throw ErrorEvent.expected(
                                                ProcessOutputException.of(exitCode, out.get(), err.get()));
                                    }
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
