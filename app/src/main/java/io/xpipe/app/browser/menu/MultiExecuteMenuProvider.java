package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class MultiExecuteMenuProvider implements BrowserMenuBranchProvider {

    protected abstract CommandBuilder createCommand(
            ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry);

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var c = createCommand(pc, model, entry);
                                        if (c == null) {
                                            continue;
                                        }

                                        var cmd = pc.command(c);
                                        model.openTerminalAsync(
                                                entry.getRawFileEntry().getName(),
                                                model.getCurrentDirectory() != null
                                                        ? model.getCurrentDirectory()
                                                                .getPath()
                                                        : null,
                                                cmd,
                                                entries.size() == 1);
                                    }
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
                new BrowserMenuLeafProvider() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        model.withShell(
                                pc -> {
                                    for (BrowserEntry entry : entries) {
                                        var c = createCommand(pc, model, entry);
                                        if (c == null) {
                                            return;
                                        }

                                        var cmd = pc.command(c);
                                        CommandDialog.runAsyncAndShow(cmd);
                                    }
                                },
                                true);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runInFileBrowser");
                    }
                },
                new BrowserMenuLeafProvider() {

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
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
