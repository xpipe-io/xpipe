package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.OsType;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.theme.Styles;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class IntroComp extends SimpleComp {

    private final String translationsKey;
    private final LabelGraphic graphic;

    @Setter
    private LabelGraphic buttonGraphic;

    @Setter
    private Runnable buttonAction;

    @Setter
    private boolean buttonDefault;

    public IntroComp(String translationsKey, LabelGraphic graphic) {
        this.translationsKey = translationsKey;
        this.graphic = graphic;
    }

    @Override
    public Region createSimple() {
        var title = new Label();
        title.textProperty().bind(AppI18n.observable(translationsKey + "Header"));
        if (OsType.getLocal() != OsType.MACOS) {
            title.getStyleClass().add(Styles.TEXT_BOLD);
        }
        AppFontSizes.title(title);

        var introDesc = new Label();
        introDesc.textProperty().bind(AppI18n.observable(translationsKey + "Content"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var img = graphic.createGraphicNode();
        if (img instanceof FontIcon fontIcon) {
            fontIcon.setIconSize(80);
        }
        var text = new VBox(title, introDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(55);
        hbox.setAlignment(Pos.CENTER);

        var button = new ButtonComp(
                AppI18n.observable(translationsKey + "Button"),
                buttonGraphic != null ? buttonGraphic.createGraphicNode() : null,
                buttonAction);
        if (buttonDefault) {
            button.styleClass(Styles.ACCENT);
        }
        var buttonPane = new StackPane(button.createRegion());
        buttonPane.setAlignment(Pos.CENTER);

        var v = new VBox(hbox, buttonPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(20);
        v.getStyleClass().add("intro");
        return v;
    }
}
