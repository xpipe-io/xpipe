package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.storage.DataStorage;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class BrowserWelcomeComp extends SimpleComp {

    private final BrowserModel model;

    public BrowserWelcomeComp(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = BrowserSavedState.load();

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome, new Spacer(Orientation.VERTICAL));

        var img = PrettyImageHelper.ofSvg(new SimpleStringProperty("Hips.svg"), 50, 75).padding(new Insets(5, 0, 0, 0)).createRegion();
        var hbox = new HBox(img, vbox);
        hbox.setSpacing(15);

        if (state == null) {
            var header = new Label("Here you will be able to see where you left off last time you exited XPipe.");
            AppFont.header(header);
            vbox.getChildren().add(header);
            hbox.setPadding(new Insets(40, 40, 40, 50));
            return new VBox(hbox);
        }

        var header = new Label("Last time you were connected to the following systems:");
        header.getStyleClass().add(Styles.TEXT_MUTED);
        AppFont.header(header);
        vbox.getChildren().add(header);

        var storeList = new VBox();
        storeList.setSpacing(8);
        state.getLastSystems().forEach(e -> {
            var entry = DataStorage.get().getStoreEntry(e.getUuid());
            if (entry.isEmpty()) {
                return;
            }

            if (!entry.get().getState().isUsable()) {
                return;
            }

            var graphic =
                    entry.get().getProvider().getDisplayIconFileName(entry.get().getStore());
            var view = PrettyImageHelper.ofFixedSquare(graphic, 45);
            view.padding(new Insets(2, 8, 2, 8));
            var tile = new Tile(
                    DataStorage.get().getStoreBrowserDisplayName(entry.get().getStore()),
                    e.getPath(),
                    view.createRegion());
            tile.setActionHandler(() -> {
                model.restoreState(e, tile.disableProperty());
            });
            storeList.getChildren().add(tile);
        });

        var sp = new ScrollPane(storeList);
        sp.setFitToWidth(true);

        var layout = new VBox();
        layout.setMaxWidth(700);
        layout.setPadding(new Insets(40, 40, 40, 50));
        layout.setSpacing(18);
        layout.getChildren().add(hbox);
        layout.getChildren().add(new Separator(Orientation.HORIZONTAL));
        layout.getChildren().add(sp);
        layout.getChildren().add(new Separator(Orientation.HORIZONTAL));

        var tile = new TileButtonComp("restore", "restoreAllSessions", "mdmz-restore", actionEvent -> {
            model.restoreState(state);
            actionEvent.consume();
        }).grow(true, false);
        layout.getChildren().add(tile.createRegion());

        return layout;
    }
}
