package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Node;
import javafx.scene.control.Button;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

@Getter
@AllArgsConstructor
public class ButtonComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<String> name;
    private final ObservableValue<LabelGraphic> graphic;
    private final Runnable listener;

    public ButtonComp(ObservableValue<String> name, Runnable listener) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(null);
        this.listener = listener;
    }

    public ButtonComp(ObservableValue<String> name, Node graphic, Runnable listener) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(new LabelGraphic.NodeGraphic(() -> graphic));
        this.listener = listener;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new Button(null);
        if (name != null) {
            name.subscribe(t -> {
                PlatformThread.runLaterIfNeeded(() -> button.setText(t));
            });
        }
        if (graphic != null) {
            graphic.subscribe(t -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    if (t == null) {
                        return;
                    }

                    var n = t.createGraphicNode();
                    button.setGraphic(n);
                    if (n instanceof FontIcon f && button.getFont() != null) {
                        f.setIconSize((int) new Size(button.getFont().getSize(), SizeUnits.PT).pixels());
                    }
                });
            });

            button.fontProperty().subscribe(c -> {
                if (button.getGraphic() instanceof FontIcon f) {
                    f.setIconSize((int) new Size(c.getSize(), SizeUnits.PT).pixels());
                }
            });
        }
        button.setOnAction(e -> getListener().run());
        button.getStyleClass().add("button-comp");
        return new SimpleCompStructure<>(button);
    }
}
