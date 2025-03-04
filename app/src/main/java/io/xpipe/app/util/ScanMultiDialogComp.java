package io.xpipe.app.util;

import io.xpipe.app.comp.base.ModalOverlayContentComp;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

import static javafx.scene.layout.Priority.ALWAYS;

class ScanMultiDialogComp extends ModalOverlayContentComp {

    private final ScanDialogBase base;

    ScanMultiDialogComp(List<DataStoreEntryRef<ShellStore>> entries, ScanDialogAction action) {
        ObservableList<DataStoreEntryRef<ShellStore>> list = FXCollections.observableArrayList(entries);
        this.base = new ScanDialogBase(true, () -> {
            var modal = getModalOverlay();
            if (modal != null) {
                modal.close();
            }
        }, action, list);
    }

    void finish() {
        ThreadHelper.runFailableAsync(() -> {
            base.finish();
        });
    }

    BooleanProperty getBusy() {
        return base.getBusy();
    }

    @Override
    protected Region createSimple() {
        var list = base.createContent();
        var b = new OptionsBuilder()
                .name("scanAlertHeader")
                .description("scanAlertHeaderDescription")
                .addComp(list.vgrow())
                .buildComp()
                .prefWidth(500)
                .prefHeight(680)
                .apply(struc -> {
                    VBox.setVgrow(struc.get().getChildren().getFirst(), ALWAYS);
                });;
        return b.createRegion();
    }
}
