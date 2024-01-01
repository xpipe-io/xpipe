package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class BrowserWelcomeComp extends SimpleComp {

    private final BrowserModel model;

    public BrowserWelcomeComp(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = model.getSavedState();

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome, new Spacer(4, Orientation.VERTICAL));
        vbox.setAlignment(Pos.CENTER_LEFT);

        var img = PrettyImageHelper.ofSvg(new SimpleStringProperty("Hips.svg"), 50, 75).padding(new Insets(5, 0, 0, 0)).createRegion();
        var hbox = new HBox(img, vbox);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(15);

        if (state == null) {
            var header = new Label("Here you will be able to see where you left off last time.");
            vbox.getChildren().add(header);
            hbox.setPadding(new Insets(40, 40, 40, 50));
            return new VBox(hbox);
        }

        var list = BindingsHelper.filteredContentBinding(state.getEntries(), e -> {
            var entry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
            if (entry.isEmpty()) {
                return false;
            }

            if (!entry.get().getValidity().isUsable()) {
                return false;
            }

            return true;
        });
        var empty = Bindings.createBooleanBinding(() -> list.isEmpty(), list);

        var header = new LabelComp(Bindings.createStringBinding(() -> {
            return !empty.get() ? "You were recently connected to the following systems:" :
                    "Here you will be able to see where you left off last time.";
        }, empty)).createRegion();
        header.getStyleClass().add(Styles.TEXT_MUTED);
        vbox.getChildren().add(header);

        var storeList = new VBox();
        storeList.setSpacing(8);

        var listBox = new ListBoxViewComp<>(list, list, e -> {
            var entry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
            var graphic = entry.get().getProvider().getDisplayIconFileName(entry.get().getStore());
            var view = PrettyImageHelper.ofFixedSquare(graphic, 45);
            view.padding(new Insets(2, 8, 2, 8));
            var content =
                    JfxHelper.createNamedEntry(DataStorage.get().getStoreDisplayName(entry.get()), e.getPath(), graphic);
            var disable = new SimpleBooleanProperty();
            return new ButtonComp(null, content, () -> {
                ThreadHelper.runAsync(() -> {
                    model.restoreState(e, disable);
                });
            }).accessibleText(DataStorage.get().getStoreDisplayName(entry.get())).disable(disable).styleClass("color-listBox").apply(struc -> struc.get().setMaxWidth(2000)).grow(true, false);
        }).apply(struc -> {
            VBox vBox = (VBox) struc.get().getContent();
            vBox.setSpacing(10);
        }).hide(empty).createRegion();

        var layout = new VBox();
        layout.getStyleClass().add("welcome");
        layout.setPadding(new Insets(40, 40, 40, 50));
        layout.setSpacing(18);
        layout.getChildren().add(hbox);
        layout.getChildren().add(Comp.separator().hide(empty).createRegion());
        layout.getChildren().add(listBox);
        VBox.setVgrow(layout.getChildren().get(2), Priority.NEVER);
        layout.getChildren().add(Comp.separator().hide(empty).createRegion());

        var tile = new TileButtonComp("restore", "restoreAllSessions", "mdmz-restore", actionEvent -> {
            model.restoreState(state);
            actionEvent.consume();
        }).grow(true, false).hide(empty).accessibleTextKey("restoreAllSessions");
        layout.getChildren().add(tile.createRegion());

        return layout;
    }
}
