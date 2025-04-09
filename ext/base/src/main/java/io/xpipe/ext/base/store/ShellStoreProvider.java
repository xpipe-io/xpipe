package io.xpipe.ext.base.store;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.store.OsLogoComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.SystemStateComp;
import io.xpipe.app.ext.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.terminal.TerminalPromptManager;
import io.xpipe.app.util.ShellStoreFormat;
import io.xpipe.ext.base.script.ScriptStoreSetup;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;

public interface ShellStoreProvider extends DataStoreProvider {

    @Override
    default ActionProvider.Action launchAction(DataStoreEntry entry) {
        return new ActionProvider.Action() {
            @Override
            public void execute() throws Exception {
                var replacement = ProcessControlProvider.get().replace(entry.ref());
                ShellStore store = replacement.getStore().asNeeded();
                var control = store.standaloneControl();
                ScriptStoreSetup.controlWithDefaultScripts(control);
                TerminalPromptManager.configurePromptScript(control);
                TerminalLauncher.open(
                        replacement.get(),
                        DataStorage.get().getStoreEntryDisplayName(replacement.get()),
                        null,
                        control);
            }
        };
    }

    @Override
    default ActionProvider.Action browserAction(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return new ActionProvider.Action() {
            @Override
            public void execute() {
                sessionModel.openFileSystemAsync(store.ref(), null, busy);
            }
        };
    }

    default Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new OsLogoComp(w, SystemStateComp.State.shellState(w));
    }

    @Override
    default DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.SHELL;
    }

    @Override
    default ObservableValue<String> informationString(StoreSection section) {
        return ShellStoreFormat.shellStore(section, state -> null);
    }
}
