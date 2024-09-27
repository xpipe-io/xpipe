package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.resources.SystemIcon;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.Hyperlinks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreIconChoiceDialogComp extends SimpleComp {

    public static void show(DataStoreEntry entry) {
        var window = AppWindowHelper.sideWindow(
                AppI18n.get("chooseCustomIcon"), stage -> new StoreIconChoiceDialogComp(entry, stage), false, null);
        window.initModality(Modality.APPLICATION_MODAL);
        window.show();
    }

    private final ObjectProperty<SystemIcon> selected = new SimpleObjectProperty<>();
    private final DataStoreEntry entry;
    private final Stage dialogStage;

    public StoreIconChoiceDialogComp(DataStoreEntry entry, Stage dialogStage) {
        this.entry = entry;
        this.dialogStage = dialogStage;
    }

    @Override
    protected Region createSimple() {
        var filterText = new SimpleStringProperty();
        var filter = new FilterComp(filterText).apply(struc -> {
            dialogStage.setOnShowing(event -> {
                struc.get().requestFocus();
                event.consume();
            });
        });
        var github = new ButtonComp(null, new FontIcon("mdi2g-github"), () -> {
                    Hyperlinks.open(Hyperlinks.SELFHST_ICONS);
                })
                .grow(false, true);
        var dialog = new DialogComp() {
            @Override
            protected void finish() {
                entry.setIcon(selected.get() != null ? selected.getValue().getIconName() : null, true);
                dialogStage.close();
            }

            @Override
            protected void discard() {}

            @Override
            public Comp<?> content() {
                return new StoreIconChoiceComp(selected, SystemIcons.getSystemIcons(), 5, filterText, () -> {
                    finish();
                });
            }

            @Override
            public Comp<?> bottom() {
                var clear = new ButtonComp(AppI18n.observable("clear"), () -> {
                            selected.setValue(null);
                            finish();
                        })
                        .grow(false, true);
                return new HorizontalComp(List.of(github, filter.hgrow(), clear)).spacing(10);
            }

            @Override
            protected Comp<?> finishButton() {
                return super.finishButton().disable(selected.isNull());
            }
        };
        dialog.prefWidth(600);
        dialog.prefHeight(600);
        return dialog.createRegion();
    }
}
