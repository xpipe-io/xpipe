package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class MultiExecuteMenuProvider implements BrowserMenuBranchProvider {

    protected abstract CommandBuilder createCommand(
            ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) throws Exception;

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        ThreadHelper.runFailableAsync(() -> {
                            var sc = model.getFileSystem().getShell().orElseThrow();
                            for (BrowserEntry entry : entries) {
                                var c = createCommand(sc, model, entry);
                                if (c == null) {
                                    continue;
                                }

                                var cmd = sc.command(c);
                                model.openTerminalAsync(entry.getRawFileEntry().getName(),
                                        model.getCurrentDirectory() != null ? model.getCurrentDirectory().getPath() : null, cmd, entries.size() == 1);
                            }
                        });
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
                        ThreadHelper.runFailableAsync(() -> {
                            var sc = model.getFileSystem().getShell().orElseThrow();
                            for (BrowserEntry entry : entries) {
                                var c = createCommand(sc, model, entry);
                                if (c == null) {
                                    return;
                                }

                                var cmd = sc.command(c);
                                CommandDialog.runAndShow(cmd);
                            }
                            model.refreshBrowserEntriesSync(entries);
                        });
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
                        ThreadHelper.runFailableAsync(() -> {
                            var sc = model.getFileSystem().getShell().orElseThrow();
                            for (BrowserEntry entry : entries) {
                                var cmd = createCommand(sc, model, entry);
                                if (cmd == null) {
                                    continue;
                                }

                                sc.command(cmd)
                                        .withWorkingDirectory(
                                                model.getCurrentDirectory().getPath())
                                        .execute();
                            }
                            model.refreshBrowserEntriesSync(entries);
                        });
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
