package io.xpipe.ext.base.store;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.OsLogoComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.ext.base.script.ScriptStore;

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
                var control = ScriptStore.controlWithDefaultScripts(
                        store.shellFunction().standaloneControl());
                control.onInit(sc -> {
                    if (entry.getStorePersistentState() instanceof ShellStoreState shellStoreState
                            && shellStoreState.getShellDialect() == null) {
                        var found = SystemIcons.detectForSystem(sc);
                        if (found.isPresent()) {
                            entry.setIcon(found.get().getIconName(), false);
                        }
                    }
                });
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
            BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
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
        return DataStoreFormatter.shellInformation(section.getWrapper());
    }
}
