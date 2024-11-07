package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.UUID;

public abstract class MultiExecuteAction implements BrowserBranchAction {

    protected abstract CommandBuilder createCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry);

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserLeafAction() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var cmd = pc.command(createCommand(pc, model, entry));
                                        if (cmd == null) {
                                            continue;
                                        }

                                        var uuid = UUID.randomUUID();
                                        model.getTerminalRequests().add(uuid);
                                        TerminalLauncher.open(
                                                model.getEntry().getEntry(),
                                                entry.getRawFileEntry().getName(),
                                                model.getCurrentDirectory() != null
                                                        ? model.getCurrentDirectory()
                                                                .getPath()
                                                        : null,
                                                cmd,
                                                uuid);
                                    }
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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
                                    for (BrowserEntry entry : entries) {
                                        var cmd = createCommand(pc, model, entry);
                                        if (cmd == null) {
                                            continue;
                                        }

                                        pc.command(cmd)
                                                .withWorkingDirectory(model.getCurrentDirectory()
                                                        .getPath())
                                                .execute();
                                    }
                                },
                                false);
                    }

                    @Override
                    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("executeInBackground");
                    }
                });
    }
}
