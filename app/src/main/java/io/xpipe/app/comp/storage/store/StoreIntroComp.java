package io.xpipe.app.comp.storage.store;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.ScanAlert;
import io.xpipe.core.impl.LocalStore;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;


public class StoreIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var title = new Label(AppI18n.get("storeIntroTitle"));
        AppFont.setSize(title, 7);
        title.getStyleClass().add("title-header");

        var introDesc = new Label(AppI18n.get("storeIntroDescription"));

        var mfi = new FontIcon("mdi2m-magnify");
        var machine = new Label(AppI18n.get("storeMachineDescription"), mfi);
        machine.heightProperty().addListener((c, o, n) -> {
            mfi.iconSizeProperty().set(n.intValue());
        });

        var scanButton = new Button(AppI18n.get("detectConnections"), new FontIcon("mdi2m-magnify"));
        scanButton.setOnAction(event -> ScanAlert.showAsync(new LocalStore(), false));
        var scanPane = new StackPane(scanButton);
        scanPane.setAlignment(Pos.CENTER);

        var dofi = new FontIcon("mdi2b-book-open-variant");
        var documentation = new Label(AppI18n.get("introDocumentation"), dofi);
        documentation.heightProperty().addListener((c, o, n) -> {
            dofi.iconSizeProperty().set(n.intValue());
        });
        var docLink = new Hyperlink(Hyperlinks.DOCUMENTATION);
        docLink.setOnAction(e -> {
            Hyperlinks.open(Hyperlinks.DOCUMENTATION);
        });
        var docLinkPane = new StackPane(docLink);
        docLinkPane.setAlignment(Pos.CENTER);

        var v = new VBox(
                title,
                introDesc,
                new Separator(Orientation.HORIZONTAL),
                machine,
                scanPane
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
        sp.setAlignment(Pos.BOTTOM_CENTER);
        sp.setPickOnBounds(false);
        sp.setPadding(new Insets(0, 0, 40, 0));
        return sp;
    }
}
