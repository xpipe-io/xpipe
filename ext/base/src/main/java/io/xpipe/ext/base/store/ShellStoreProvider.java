package io.xpipe.ext.base.store;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.OsLogoComp;
import io.xpipe.app.comp.base.SystemStateComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.base.script.ScriptStore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;

public interface ShellStoreProvider extends DataStoreProvider {

    @Override
    default ActionProvider.Action launchAction(DataStoreEntry entry) {
        return new ActionProvider.Action() {
            @Override
            public void execute() throws Exception {
                ShellStore store = entry.getStore().asNeeded();
                TerminalLauncher.open(entry, DataStorage.get().getStoreEntryDisplayName(entry), null, ScriptStore.controlWithDefaultScripts(store.control()));
            }
        };
    }

    @Override
    default ActionProvider.Action browserAction(BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return new ActionProvider.Action() {
            @Override
            public void execute() throws Exception {
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
