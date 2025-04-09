package io.xpipe.app.util;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

import java.util.List;

public class ScanDialog {

    public static void showAsync(DataStoreEntry entry) {
        var showForCon = entry == null
                || (entry.getStore() instanceof ShellStore
                        && (!(entry.getStorePersistentState() instanceof SystemState systemState)
                                || systemState.getTtyState() == null
                                || systemState.getTtyState() == ShellTtyState.NONE));
        if (showForCon) {
            show(entry, ScanDialogAction.shellScanAction());
        }
    }

    private static void show(DataStoreEntry initialStore, ScanDialogAction action) {
        var comp = new ScanSingleDialogComp(initialStore != null ? initialStore.ref() : null, action);
        var modal = ModalOverlay.of("scanAlertTitle", comp);
        var button = new ModalButton(
                "ok",
                () -> {
                    comp.finish();
                },
                false,
                true);
        button.augment(r -> r.disableProperty().bind(PlatformThread.sync(comp.getBusy())));
        modal.addButton(button);
        modal.show();
    }

    public static void showMulti(List<DataStoreEntryRef<ShellStore>> entries, ScanDialogAction action) {
        var comp = new ScanMultiDialogComp(entries, action);
        var modal = ModalOverlay.of("scanAlertTitle", comp);
        var queueEntry = new AppLayoutModel.QueueEntry(AppI18n.observable("scanConnections"), new LabelGraphic.IconGraphic("mdi2l-layers-plus"), () -> {});
        var button = new ModalButton(
                "ok",
                () -> {
                    modal.hide();
                    AppLayoutModel.get().getQueueEntries().add(queueEntry);
                    ThreadHelper.runAsync(() -> {
                        comp.finish();
                        modal.hide();
                        AppLayoutModel.get().getQueueEntries().remove(queueEntry);
                    });
                },
                false,
                true);
        button.augment(r -> r.disableProperty().bind(PlatformThread.sync(comp.getBusy())));
        modal.addButton(button);
        modal.show();
    }
}
