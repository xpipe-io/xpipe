package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

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
            var comp = Comp.of(() -> {
                var l = new TextArea();
                l.textProperty().bind(text);
                l.setWrapText(true);
                l.getStyleClass().add("error-overlay-comp");
                l.setEditable(false);
                return l;
            });
            content.set(new ModalOverlayComp.OverlayContent("error", comp, null, () -> {}));
        });
        content.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                this.text.setValue(null);
            }
        });
        return new ModalOverlayComp(background, content).createRegion();
    }
}
