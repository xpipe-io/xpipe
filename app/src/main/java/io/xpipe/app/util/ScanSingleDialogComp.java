package io.xpipe.app.util;

import io.xpipe.app.comp.base.ModalOverlayContentComp;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

class ScanSingleDialogComp extends ModalOverlayContentComp {

    private final DataStoreEntryRef<ShellStore> initialStore;
    private final ObjectProperty<DataStoreEntryRef<ShellStore>> entry;
    private final ScanDialogBase base;

    @SuppressWarnings("unchecked")
    ScanSingleDialogComp(DataStoreEntryRef<ShellStore> entry, ScanDialogAction action) {
        this.initialStore = entry;
        this.entry = new SimpleObjectProperty<>(entry);

        ObservableList<DataStoreEntryRef<ShellStore>> list = FXCollections.observableArrayList();
        this.entry.subscribe(v -> {
            if (v != null) {
                list.setAll(v);
            } else {
                list.clear();
            }
        });
        this.base = new ScanDialogBase(
                true,
                () -> {
                    var modal = getModalOverlay();
                    if (initialStore != null && modal != null) {
                        modal.close();
                    }
                },
                action,
                list);
    }

    void finish() {
        try {
            base.finish();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    BooleanProperty getBusy() {
        return base.getBusy();
    }

    @Override
    protected Region createSimple() {
        var list = base.createComp();
        var b = new OptionsBuilder()
                .name("scanAlertChoiceHeader")
                .description("scanAlertChoiceHeaderDescription")
                .addComp(new StoreChoiceComp<>(
                                StoreChoiceComp.Mode.OTHER,
                                null,
                                entry,
                                ShellStore.class,
                                store1 -> true,
                                StoreViewState.get().getAllConnectionsCategory())
                        .disable(base.getBusy().or(new SimpleBooleanProperty(initialStore != null))))
                .name("scanAlertHeader")
                .description("scanAlertHeaderDescription")
                .addComp(list.vgrow())
                .buildComp()
                .prefWidth(500)
                .prefHeight(680);
        return b.createRegion();
    }
}
