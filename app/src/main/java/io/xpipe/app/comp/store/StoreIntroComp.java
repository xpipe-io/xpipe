package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanAlert;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var title = new Label(AppI18n.get("storeIntroTitle"));
        AppFont.setSize(title, 7);

        var introDesc = new Label(AppI18n.get("storeIntroDescription"));

        var mfi = new FontIcon("mdi2p-playlist-plus");
        var machine = new Label(AppI18n.get("storeMachineDescription"));
        machine.heightProperty().addListener((c, o, n) -> {
            mfi.iconSizeProperty().set(n.intValue());
        });

        var scanButton = new Button(AppI18n.get("detectConnections"), new FontIcon("mdi2m-magnify"));
        scanButton.setOnAction(event -> ScanAlert.showAsync(DataStorage.get().local()));
        var scanPane = new StackPane(scanButton);
        scanPane.setAlignment(Pos.CENTER);

        var img = PrettyImageHelper.ofSvg(new SimpleStringProperty("Wave.svg"), 80, 150).createRegion();
        var text = new VBox(title, introDesc, new Separator(Orientation.HORIZONTAL), machine);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(35);
        hbox.setAlignment(Pos.CENTER);

        var v = new VBox(
                hbox, scanPane
                //                new Separator(Orientation.HORIZONTAL),
                //                documentation,
                //                docLinkPane
                );
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");

        var sp = new StackPane(v);
        sp.setAlignment(Pos.CENTER);
        sp.setPickOnBounds(false);
        return sp;
    }
}
