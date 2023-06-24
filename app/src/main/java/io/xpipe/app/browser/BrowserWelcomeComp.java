package io.xpipe.app.browser;

import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.storage.DataStorage;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class BrowserWelcomeComp extends SimpleComp {

    private final BrowserModel model;

    public BrowserWelcomeComp(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = BrowserSavedState.load();

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome);
        vbox.setMaxWidth(700);
        vbox.setPadding(new Insets(40, 40, 40, 50));
        vbox.setSpacing(18);
        if (state == null) {
            var header = new Label("Here you will be able to see were you left off last time you exited XPipe.");
            AppFont.header(header);
            vbox.getChildren().add(header);
            return vbox;
        }

        var header = new Label("Last time you were connected to the following systems:");
        header.getStyleClass().add(Styles.TEXT_MUTED);
        AppFont.header(header);
        vbox.getChildren().add(header);

        var storeList = new VBox();
        storeList.setSpacing(8);
        state.getLastSystems().forEach(e-> {
            var entry = DataStorage.get().getStoreEntry(e.getUuid());
            if (entry.isEmpty()) {
                return;
            }

            var graphic =
                    entry.get().getProvider().getDisplayIconFileName(entry.get().getStore());
            var view = new PrettyImageComp(new SimpleStringProperty(graphic), 45, 45);
            var openButton = new Button(null, new FontIcon("mdmz-restore"));
            new FancyTooltipAugment<>("restore").augment(openButton);
            openButton.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_CIRCLE);
            openButton.setOnAction(event -> {
                model.restoreState(e, openButton.disableProperty());
                event.consume();
            });
            var tile = new Tile(entry.get().getName(), e.getPath(), view.createRegion());
            tile.setAction(openButton);
            storeList.getChildren().add(tile);
        });

        var sp = new ScrollPane(storeList);
        sp.setFitToWidth(true);
        vbox.getChildren().add(sp);
        vbox.getChildren().add(new Separator(Orientation.HORIZONTAL));

        var tile = new TileButtonComp("restore", "restoreAllSessions", "mdmz-restore", actionEvent -> {
            model.restoreState(state);
            actionEvent.consume();
        }).grow(true, false);
        vbox.getChildren().add(tile.createRegion());

        return vbox;
    }
}
