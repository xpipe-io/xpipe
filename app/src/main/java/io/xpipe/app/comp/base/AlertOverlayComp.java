package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
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

public class AlertOverlayComp extends SimpleComp {


    public AlertOverlayComp(Comp<?> background, Property<OverlayContent> overlayContent) {
        this.background = background;
        this.overlayContent = overlayContent;
    }

    @Value
    public static class OverlayContent {

        ObservableValue<String> title;
        Comp<?> content;
        Runnable onFinish;
    }

    private final Comp<?> background;
    private final Property<OverlayContent> overlayContent;

    @Override
    protected Region createSimple() {
        var bgRegion = background.createRegion();
        var pane = new StackPane(bgRegion);
        pane.setPickOnBounds(false);
        PlatformThread.sync(overlayContent).addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                pane.getChildren().remove(1);
            }

            if (newValue != null) {
                var r = newValue.content.createRegion();

                var finishButton = new Button("Finish");
                Styles.toggleStyleClass(finishButton, Styles.FLAT);
                finishButton.setOnAction(event -> {
                    newValue.onFinish.run();
                    overlayContent.setValue(null);
                });

                var buttonBar = new ButtonBar();
                buttonBar.getButtons().addAll(finishButton);
                var box = new VBox(r, buttonBar);
                box.setSpacing(15);
                box.setPadding(new Insets(15));
                var tp = new TitledPane(newValue.title.getValue(), box);
                tp.setMaxWidth(400);
                tp.setCollapsible(false);
                tp.getStyleClass().add("elevated-3");

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
                stack.setOnMouseClicked(event -> {
                    if (overlayContent.getValue() != null) {
                        overlayContent.setValue(null);
                    }
                });
                stack.setAlignment(Pos.CENTER);
                close.maxWidthProperty().bind(tp.widthProperty());
                close.maxHeightProperty().bind(tp.heightProperty());

                pane.getChildren().add(stack);
            }
        });
        return pane;
    }
}
