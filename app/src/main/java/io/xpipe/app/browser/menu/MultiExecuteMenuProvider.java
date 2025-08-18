package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.action.impl.RunCommandInBackgroundActionProvider;
import io.xpipe.app.browser.action.impl.RunCommandInBrowserActionProvider;
import io.xpipe.app.browser.action.impl.RunCommandInTerminalActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;

import javafx.beans.value.ObservableValue;

import lombok.SneakyThrows;

import java.util.List;

public abstract class MultiExecuteMenuProvider implements BrowserMenuBranchProvider {

    protected abstract List<CommandBuilder> createCommand(BrowserFileSystemTabModel model, List<BrowserEntry> entries);

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {

                    @Override
                    @SneakyThrows
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (model.getCurrentPath().getValue() == null) {
                            return;
                        }

                        var commands = createCommand(model, entries);
                        for (CommandBuilder command : commands) {
                            var builder = RunCommandInTerminalActionProvider.Action.builder();
                            builder.initFiles(
                                    model, List.of(model.getCurrentPath().getValue()));
                            builder.command(command.buildFull(
                                    model.getFileSystem().getShell().orElseThrow()));
                            builder.build().executeAsync();
                        }
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppPrefs.get().terminalType().getValue() != null;
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var t = AppPrefs.get().terminalType().getValue();
                        return AppI18n.observable(
                                "executeInTerminal",
                                t != null ? t.toTranslatedString().getValue() : "?");
                    }
                },
                new BrowserMenuLeafProvider() {

                    @Override
                    @SneakyThrows
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var commands = createCommand(model, entries);
                        for (CommandBuilder command : commands) {
                            var builder = RunCommandInBrowserActionProvider.Action.builder();
                            builder.initFiles(
                                    model, List.of(model.getCurrentPath().getValue()));
                            builder.command(command.buildFull(
                                    model.getFileSystem().getShell().orElseThrow()));
                            builder.build().executeAsync();
                        }
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runInFileBrowser");
                    }
                },
                new BrowserMenuLeafProvider() {

                    @Override
                    @SneakyThrows
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var commands = createCommand(model, entries);
                        for (CommandBuilder command : commands) {
                            var builder = RunCommandInBackgroundActionProvider.Action.builder();
                            builder.initFiles(
                                    model, List.of(model.getCurrentPath().getValue()));
                            builder.command(command.buildFull(
                                    model.getFileSystem().getShell().orElseThrow()));
                            builder.build().executeAsync();
                        }
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
