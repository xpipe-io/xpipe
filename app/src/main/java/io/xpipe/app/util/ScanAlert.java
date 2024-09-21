package io.xpipe.app.util;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.ShellValidationContext;
import io.xpipe.core.store.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ScanAlert {

    public static void showAsync(DataStoreEntry entry, ValidationContext<?> context) {
        ThreadHelper.runAsync(() -> {
            var showForCon = entry == null
                    || (entry.getStore() instanceof ShellStore
                            && (!(entry.getStorePersistentState() instanceof ShellStoreState shellStoreState)
                                    || shellStoreState.getTtyState() == null
                                    || shellStoreState.getTtyState() == ShellTtyState.NONE));
            if (showForCon) {
                showForShellStore(entry, (ShellValidationContext) context);
            }
        });
    }

    public static void showForShellStore(DataStoreEntry initial, ShellValidationContext context) {
        show(
                initial,
                (DataStoreEntry entry, ShellControl sc) -> {
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
                    var applicable = new ArrayList<ScanProvider.ScanOperation>();
                    for (ScanProvider scanProvider : providers) {
                        try {
                            // Previous scan operation could have exited the shell
                            sc.start();
                            ScanProvider.ScanOperation operation = scanProvider.create(entry, sc);
                            if (operation != null) {
                                applicable.add(operation);
                            }
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).handle();
                        }
                    }
                    return applicable;
                },
                context);
    }

    private static void show(
            DataStoreEntry initialStore,
            BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOperation>> applicable,
            ShellValidationContext shellValidationContext) {
        DialogComp.showWindow(
                "scanAlertTitle",
                stage -> new ScanDialog(
                        stage, initialStore != null ? initialStore.ref() : null, applicable, shellValidationContext));
    }

}
