package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import atlantafx.base.theme.Styles;
import javafx.scene.paint.Color;
import lombok.Setter;
import org.kordamp.ikonli.javafx.FontIcon;

public class IntroComp extends SimpleRegionBuilder {

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
        title.getStyleClass().add(Styles.TEXT_BOLD);
        AppFontSizes.title(title);

        var introDesc = new Label();
        introDesc.textProperty().bind(AppI18n.observable(translationsKey + "Content"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var img = graphic.createGraphicNode();
        if (img instanceof FontIcon fontIcon) {
            fontIcon.setIconSize(80);
        }
        var hideImg = Bindings.not(AppSizeBreakpoints.compactMode());
        img.managedProperty().bind(hideImg);
        img.visibleProperty().bind(hideImg);

        var text = new VBox(title, introDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(55);
        hbox.setAlignment(Pos.CENTER);
        var v = new VBox(hbox);

        if (buttonAction != null) {
            var button = new ButtonComp(AppI18n.observable(translationsKey + "Button"), buttonGraphic, buttonAction);
            if (buttonDefault) {
                button.style(Styles.ACCENT);
            }
            var buttonPane = new StackPane(button.build());
            buttonPane.setAlignment(Pos.CENTER);
            v.getChildren().add(buttonPane);
        }

        v.setMinWidth(0);
        v.setMinHeight(0);

        v.setSpacing(20);
        v.getStyleClass().add("intro");
        v.setPadding(new Insets(40, 20, 40, 20));
        return v;
    }
}
