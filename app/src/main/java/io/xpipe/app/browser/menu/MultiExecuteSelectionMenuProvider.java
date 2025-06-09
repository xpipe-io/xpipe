package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.action.impl.RunCommandInBackgroundActionProvider;
import io.xpipe.app.browser.action.impl.RunCommandInBrowserActionProvider;
import io.xpipe.app.browser.action.impl.RunCommandInTerminalActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class MultiExecuteSelectionMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public boolean isMutation() {
        return true;
    }

    protected abstract String createCommand(BrowserFileSystemTabModel model);

    protected abstract String getTerminalTitle();

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var builder = RunCommandInTerminalActionProvider.Action.builder();
                        builder.initEntries(model, entries);
                        builder.title(getTerminalTitle());
                        builder.command(createCommand(model));
                        builder.build().executeAsync();
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
                        var builder = RunCommandInBrowserActionProvider.Action.builder();
                        builder.initEntries(model, entries);
                        builder.command(createCommand(model));
                        builder.build().executeAsync();
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
                        var builder = RunCommandInBackgroundActionProvider.Action.builder();
                        builder.initEntries(model, entries);
                        builder.command(createCommand(model));
                        builder.build().executeAsync();
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("runSilent");
                    }
                });
    }
}
