package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;

import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Node;
import javafx.scene.control.Button;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

@Getter
public class ButtonComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<String> name;
    private final ObjectProperty<Node> graphic;
    private final Runnable listener;

    public ButtonComp(ObservableValue<String> name, Runnable listener) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(null);
        this.listener = listener;
    }

    public ButtonComp(ObservableValue<String> name, Node graphic, Runnable listener) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(graphic);
        this.listener = listener;
    }

    public Node getGraphic() {
        return graphic.get();
    }

    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new Button(null);
        if (name != null) {
            button.textProperty().bind(PlatformThread.sync(name));
        }
        var graphic = getGraphic();
        if (graphic instanceof FontIcon f) {
            // f.iconColorProperty().bind(button.textFillProperty());
            button.fontProperty().subscribe(c -> {
                f.setIconSize((int) new Size(c.getSize(), SizeUnits.PT).pixels());
            });
        }

        button.setGraphic(getGraphic());
        button.setOnAction(e -> getListener().run());
        button.getStyleClass().add("button-comp");
        return new SimpleCompStructure<>(button);
    }
}
