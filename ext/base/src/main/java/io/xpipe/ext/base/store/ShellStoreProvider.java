package io.xpipe.ext.base.store;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.store.DataStoreUsageCategory;
import io.xpipe.app.store.ShellStore;
import io.xpipe.app.store.StatefulDataStore;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.terminal.TerminalPromptManager;
import io.xpipe.app.util.FailableRunnable;
import io.xpipe.ext.base.script.ScriptStoreSetup;

import javafx.beans.property.BooleanProperty;

import java.util.UUID;

public interface ShellStoreProvider extends DataStoreProvider {

    @Override
    default FailableRunnable<Exception> launch(DataStoreEntry entry) {
        return () -> {
            var replacement = ProcModuleProvider.get().replace(entry.ref());
            ShellStore store = replacement.getStore().asNeeded();
            var control = store.standaloneControl();
            // These prepend scripts, not append
            TerminalPromptManager.configurePromptScript(control);
            ScriptStoreSetup.controlWithDefaultScripts(control);
            var request = UUID.randomUUID();
            TerminalLaunch.builder()
                    .request(request)
                    .entry(replacement.get())
                    .command(control)
                    .launch();
        };
    }

    @Override
    default FailableRunnable<Exception> launchBrowser(
            BrowserFullSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return () -> {
            sessionModel.openFileSystemAsync(store.ref(), null, null, busy);
        };
    }

    @Override
    default DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.SHELL;
    }

    @Override
    default StoreEntryInformation buildInformation(StoreSection section) {
        var st = (ShellStore) section.getEntry().getStore();
        if (st instanceof StatefulDataStore<?> sds && sds.getState() instanceof SystemState ss) {
            var base = StoreEntryInformation.ofSystemState(ss);
            return base.append(buildAdditionalInformation(section, ss));
        } else {
            return null;
        }
    }

    default StoreEntryInformation buildAdditionalInformation(StoreSection section, SystemState state) {
        return StoreEntryInformation.of();
    }
}
