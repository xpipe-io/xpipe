package io.xpipe.app.util;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ScanAlert {

    public static void showAsync(DataStoreEntry entry) {
        ThreadHelper.runAsync(() -> {
            var showForCon = entry == null
                    || (entry.getStore() instanceof ShellStore
                            && (!(entry.getStorePersistentState() instanceof SystemState systemState)
                                    || systemState.getTtyState() == null
                                    || systemState.getTtyState() == ShellTtyState.NONE));
            if (showForCon) {
                showForShellStore(entry);
            }
        });
    }

    public static void showForShellStore(DataStoreEntry initial) {
        show(initial, (DataStoreEntry entry, ShellControl sc) -> {
            if (!sc.canHaveSubshells()) {
                return null;
            }

            if (!sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                return null;
            }

            if (sc.getTtyState() != ShellTtyState.NONE) {
                return null;
            }

            var providers = ScanProvider.getAll();
            var applicable = new ArrayList<ScanProvider.ScanOpportunity>();
            for (ScanProvider scanProvider : providers) {
                try {
                    // Previous scan operation could have exited the shell
                    sc.start();
                    ScanProvider.ScanOpportunity operation = scanProvider.create(entry, sc);
                    if (operation != null) {
                        applicable.add(operation);
                    }
                } catch (Exception ex) {
                    ErrorEvent.fromThrowable(ex).handle();
                }
            }
            return applicable;
        });
    }

    private static void show(
            DataStoreEntry initialStore,
            BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOpportunity>> applicable) {
        DialogComp.showWindow(
                "scanAlertTitle",
                stage -> new ScanDialog(stage, initialStore != null ? initialStore.ref() : null, applicable));
    }
}
