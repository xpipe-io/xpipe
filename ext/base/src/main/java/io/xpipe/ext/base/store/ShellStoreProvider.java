package io.xpipe.ext.base.store;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.OsLogoComp;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.hub.comp.SystemStateComp;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.terminal.TerminalPromptManager;
import io.xpipe.app.util.StoreStateFormat;
import io.xpipe.core.FailableRunnable;
import io.xpipe.ext.base.script.ScriptStoreSetup;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public interface ShellStoreProvider extends DataStoreProvider {

    @Override
    default FailableRunnable<Exception> launch(DataStoreEntry entry) {
        return () -> {
            var replacement = ProcessControlProvider.get().replace(entry.ref());
            ShellStore store = replacement.getStore().asNeeded();
            var control = store.standaloneControl();
            // These prepend scripts, not append
            TerminalPromptManager.configurePromptScript(control);
            ScriptStoreSetup.controlWithDefaultScripts(control);
            TerminalLaunch.builder().entry(replacement.get()).command(control).launch();
        };
    }

    @Override
    default FailableRunnable<Exception> launchBrowser(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return () -> {
            sessionModel.openFileSystemAsync(store.ref(), null, null, busy);
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
        return StoreStateFormat.shellStore(
                section, state -> formatAdditionalInformation(section, state).toArray(String[]::new), null);
    }

    default List<String> formatAdditionalInformation(StoreSection section, SystemState state) {
        return List.of();
    }
}
