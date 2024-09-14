package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.resources.SystemIcon;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class StoreIconChoiceDialogComp extends SimpleComp {

    public static void show(DataStoreEntry entry) {
        SystemIcons.load();
        var icon = new SimpleObjectProperty<>(SystemIcons.getForId(entry.getIcon()).orElse(null));
        var window = AppWindowHelper.sideWindow(AppI18n.get("chooseCustomIcon"), stage -> new StoreIconChoiceDialogComp(icon, entry,stage),true,null);
        window.show();
    }

    private final Property<SystemIcon> selected;
    private final DataStoreEntry entry;
    private final Stage dialogStage;

    public StoreIconChoiceDialogComp(Property<SystemIcon> selected, DataStoreEntry entry, Stage dialogStage) {
        this.selected = selected;
        this.entry = entry;
        this.dialogStage = dialogStage;
    }

    @Override
    protected Region createSimple() {
        var filterText = new SimpleStringProperty();
        var table = new StoreIconChoiceComp(selected, SystemIcons.getSystemIcons(), 5, filterText);
        var filter = new FilterComp(filterText).apply(struc -> {
            dialogStage.setOnShowing(event -> {
                struc.get().requestFocus();
                event.consume();
            });
        });
        var dialog = new DialogComp() {
            @Override
            protected void finish() {
                var icon = selected.getValue().getIconName();
                entry.setIcon(icon);
                dialogStage.close();
            }

            @Override
            public Comp<?> content() {
                return table;
            }

            @Override
            public Comp<?> bottom() {
                return filter;
            }
        };
        dialog.prefWidth(600);
        dialog.prefHeight(600);
        return dialog.createRegion();
    }
}
