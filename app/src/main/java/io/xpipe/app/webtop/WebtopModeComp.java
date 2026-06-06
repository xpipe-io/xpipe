package io.xpipe.app.webtop;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class WebtopModeComp extends SimpleRegionBuilder {

    public static void showDialog() {
        var modal = ModalOverlay.of("webtopMode", new WebtopModeComp().prefWidth(600));
        modal.show();
    }

    @Override
    protected Region createSimple() {
        var desktop = new ToggleButton();
        desktop.setGraphic(new FontIcon("mdi2d-desktop-classic"));
        desktop.textProperty().bind(AppI18n.observable("desktopMode"));
        desktop.setAlignment(Pos.CENTER);
        desktop.setContentDisplay(ContentDisplay.TOP);
        desktop.setWrapText(true);

        var mobile = new ToggleButton();
        mobile.setGraphic(new FontIcon("mdi2t-tablet"));
        mobile.textProperty().bind(AppI18n.observable("mobileMode"));
        mobile.setAlignment(Pos.CENTER);
        mobile.setContentDisplay(ContentDisplay.TOP);
        mobile.setWrapText(true);

        var group = new ToggleGroup();
        group.getToggles().addAll(desktop, mobile);
        group.selectToggle(WebtopMode.isMobile() ? mobile : desktop);
        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null) {
                return;
            }

            if (newValue == null) {
                group.selectToggle(oldValue);
                return;
            }

            WebtopMode.set(newValue == mobile);
        });

        var hbox = new HBox(desktop, mobile);
        hbox.setSpacing(20);
        hbox.setAlignment(Pos.CENTER);

        hbox.getStyleClass().add("webtop-mode");

        return hbox;
    }
}
