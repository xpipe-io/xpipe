package io.xpipe.app.comp.base;

import com.jfoenix.controls.JFXButton;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

public class ButtonComp extends Comp<CompStructure<JFXButton>> {

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

    public ObservableValue<String> getName() {
        return name;
    }

    public Node getGraphic() {
        return graphic.get();
    }

    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public Runnable getListener() {
        return listener;
    }

    @Override
    public CompStructure<JFXButton> createBase() {
        var button = new JFXButton(null);
        if (name != null) {
            button.textProperty().bind(name);
        }
        var graphic = getGraphic();
        if (graphic instanceof FontIcon f) {
            f.iconColorProperty().bind(button.textFillProperty());
            SimpleChangeListener.apply(button.fontProperty(), c -> {
                f.setIconSize((int) new Size(c.getSize(), SizeUnits.PT).pixels());
            });
        }

        button.setGraphic(getGraphic());
        button.setOnAction(e -> getListener().run());
        button.getStyleClass().add("button-comp");
        return new SimpleCompStructure<>(button);
    }
}
