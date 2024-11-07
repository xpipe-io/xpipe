package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.kordamp.ikonli.javafx.FontIcon;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ErrorOverlayComp extends SimpleComp {

    Comp<?> background;
    Property<String> text;

    public ErrorOverlayComp(Comp<?> background, Property<String> text) {
        this.background = background;
        this.text = text;
    }

    @Override
    protected Region createSimple() {
        var content = new SimpleObjectProperty<ModalOverlayComp.OverlayContent>();
        this.text.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var comp = Comp.of(() -> {
                    var l = new TextArea();
                    l.textProperty().bind(PlatformThread.sync(text));
                    l.setWrapText(true);
                    l.getStyleClass().add("error-overlay-comp");
                    l.setEditable(false);
                    return l;
                });
                content.set(new ModalOverlayComp.OverlayContent(
                        "error",
                        comp,
                        Comp.of(() -> {
                            var graphic = new FontIcon("mdomz-warning");
                            graphic.setIconColor(Color.RED);
                            return new StackPane(graphic);
                        }),
                        null,
                        () -> {},
                        false));
            });
        });
        content.addListener((observable, oldValue, newValue) -> {
            // Handle close
            if (newValue == null) {
                this.text.setValue(null);
            }
        });
        return new ModalOverlayComp(background, content).createRegion();
    }
}
