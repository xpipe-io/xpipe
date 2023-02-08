package io.xpipe.app.comp.storage.source;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class SourceStorageEmptyIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var title = new Label(I18n.get("introTitle"));
        AppFont.setSize(title, 7);
        title.getStyleClass().add("title-header");

        var descFi = new FontIcon("mdi2i-information-outline");
        var introDesc = new Label(I18n.get("introDescription"));
        introDesc.heightProperty().addListener((c, o, n) -> {
            descFi.iconSizeProperty().set(n.intValue());
        });

        var fi = new FontIcon("mdi2f-folder-plus-outline");
        var addCollection = new Label(I18n.get("introCollection"), fi);
        addCollection.heightProperty().addListener((c, o, n) -> {
            fi.iconSizeProperty().set(n.intValue());
        });

        var pipeFi = new FontIcon("mdi2p-pipe-disconnected");
        var pipe = new Label(I18n.get("introPipe"), pipeFi);
        pipe.heightProperty().addListener((c, o, n) -> {
            pipeFi.iconSizeProperty().set(n.intValue());
        });

        var dfi = new FontIcon("mdi2b-book-open-variant");
        var documentation = new Label(I18n.get("introDocumentation"), dfi);
        documentation.heightProperty().addListener((c, o, n) -> {
            dfi.iconSizeProperty().set(n.intValue());
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
                addCollection,
                new Separator(Orientation.HORIZONTAL),
                pipe,
                new Separator(Orientation.HORIZONTAL),
                documentation,
                docLinkPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");

        var sp = new StackPane(v);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }
}
