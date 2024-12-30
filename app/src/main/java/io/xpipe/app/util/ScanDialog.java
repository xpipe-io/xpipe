package io.xpipe.app.util;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ScanDialog {

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
        var action = new ScanDialogAction() {

            @Override
            public boolean scan(ObservableList<ScanProvider.ScanOpportunity> all, ObservableList<ScanProvider.ScanOpportunity> selected, DataStoreEntry entry, ShellControl sc) {
                if (!sc.canHaveSubshells()) {
                    return false;
                }

                if (!sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                    return false;
                }

                if (sc.getTtyState() != ShellTtyState.NONE) {
                    return false;
                }

                var providers = ScanProvider.getAll();
                for (ScanProvider scanProvider : providers) {
                    try {
                        // Previous scan operation could have exited the shell
                        sc.start();
                        ScanProvider.ScanOpportunity operation = scanProvider.create(entry, sc);
                        if (operation != null) {
                            if (!operation.isDisabled() && operation.isDefaultSelected()) {
                                selected.add(operation);
                            }
                            all.add(operation);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
                return true;
            }
        };
        show(initial, action);
    }

    private static void show(
            DataStoreEntry initialStore,
            ScanDialogAction action) {
        var comp = new ScanDialogComp(initialStore != null ? initialStore.ref() : null, action);
        var modal = ModalOverlay.of("scanAlertTitle", comp);
        modal.addButton(ModalButton.ok(() -> {
            comp.finish();
        }));
        modal.showAndWait();
    }
}
