package io.xpipe.app.comp.base;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

public class ModalOverlayComp extends SimpleComp {


    public ModalOverlayComp(Comp<?> background, Property<OverlayContent> overlayContent) {
        this.background = background;
        this.overlayContent = overlayContent;
    }

    @Value
    public static class OverlayContent {

        String titleKey;
        Comp<?> content;
        String finishKey;
        Runnable onFinish;
    }

    private final Comp<?> background;
    private final Property<OverlayContent> overlayContent;

    @Override
    protected Region createSimple() {
        var bgRegion = background.createRegion();
        var modal = new ModalPane();
        modal.getStyleClass().add("modal-overlay-comp");
        var pane = new StackPane(bgRegion, modal);
        pane.setPickOnBounds(false);
        PlatformThread.sync(overlayContent).addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                modal.hide(true);
            }

            if (newValue != null) {
                var r = newValue.content.createRegion();
                var box = new VBox(r);
                box.setSpacing(15);
                box.setPadding(new Insets(15));

                if (newValue.finishKey != null) {
                    var finishButton = new Button(AppI18n.get(newValue.finishKey));
                    Styles.toggleStyleClass(finishButton, Styles.FLAT);
                    finishButton.setOnAction(event -> {
                        newValue.onFinish.run();
                        overlayContent.setValue(null);
                    });

                    var buttonBar = new ButtonBar();
                    buttonBar.getButtons().addAll(finishButton);
                    box.getChildren().add(buttonBar);
                }

                var tp = new TitledPane(AppI18n.get(newValue.titleKey), box);
                tp.setMaxWidth(400);
                tp.setCollapsible(false);

                var closeButton = new Button(null, new FontIcon("mdi2w-window-close"));
                closeButton.setOnAction(event -> {
                    overlayContent.setValue(null);
                });
                Shortcuts.addShortcut(closeButton, new KeyCodeCombination(KeyCode.ESCAPE));
                Styles.toggleStyleClass(closeButton, Styles.FLAT);
                var close = new AnchorPane(closeButton);
                close.setPickOnBounds(false);
                AnchorPane.setTopAnchor(closeButton, 10.0);
                AnchorPane.setRightAnchor(closeButton, 10.0);

                var stack = new StackPane(tp, close);
                stack.setPadding(new Insets(10));
                stack.setOnMouseClicked(event -> {
                    if (overlayContent.getValue() != null) {
                        overlayContent.setValue(null);
                    }
                });
                stack.setAlignment(Pos.CENTER);
                close.maxWidthProperty().bind(tp.widthProperty());
                close.maxHeightProperty().bind(tp.heightProperty());

                modal.show(stack);
            }
        });
        return pane;
    }
}
