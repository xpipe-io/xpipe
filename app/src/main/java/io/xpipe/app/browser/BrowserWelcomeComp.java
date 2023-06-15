package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.storage.DataStorage;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
        var showList = state.getLastSystems().size() > 1
                || (state.getLastSystems().size() == 1
                        && state.getLastSystems().values().stream().allMatch(s -> s != null));

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome);
        vbox.setPadding(new Insets(40, 40, 40, 50));
        vbox.setSpacing(18);
        if (!showList) {
            return vbox;
        }

        var header = new Label("Last time you were connected to the following systems:");
        AppFont.header(header);
        vbox.getChildren().add(header);

        var storeList = new VBox();
        storeList.setPadding(new Insets(0, 0, 0, 10));
        storeList.setSpacing(8);
        state.getLastSystems().forEach((uuid, s) -> {
            var entry = DataStorage.get().getStoreEntry(uuid);
            if (entry.isEmpty()) {
                return;
            }

            var graphic =
                    entry.get().getProvider().getDisplayIconFileName(entry.get().getStore());
            var view = new PrettyImageComp(new SimpleStringProperty(graphic), 24, 24);
            var l = new Label(entry.get().getName() + (s != null ? ":   " + s : ""), view.createRegion());
            l.setGraphicTextGap(10);
            storeList.getChildren().add(l);
        });

        vbox.getChildren().add(storeList);
        vbox.getChildren().add(new Spacer(20));

        var restoreLabel = new Label("Do you want to restore these sessions?");
        AppFont.header(restoreLabel);
        vbox.getChildren().add(restoreLabel);

        var restoreButton = new Button("Restore sessions");
        vbox.getChildren().add(restoreButton);

        return vbox;
    }
}
