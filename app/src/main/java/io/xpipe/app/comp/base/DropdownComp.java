package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DropdownComp extends Comp <CompStructure<MenuButton>>{

    private final ObservableValue<String> name;
    private final ObjectProperty<Node> graphic;
    private final List<Comp<?>> items;

    public DropdownComp(ObservableValue<String> name, List<Comp<?>> items) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(null);
        this.items = items;
    }

    public DropdownComp(ObservableValue<String> name, Node graphic, List<Comp<?>> items) {
        this.name = name;
        this.graphic = new SimpleObjectProperty<>(graphic);
        this.items = items;
    }

    public Node getGraphic() {
        return graphic.get();
    }

    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    @Override
    public CompStructure<MenuButton> createBase() {
        var button = new MenuButton(null);
        if (name != null) {
            button.textProperty().bind(name);
        }
        var graphic = getGraphic();
        if (graphic instanceof FontIcon f) {
            SimpleChangeListener.apply(button.fontProperty(), c -> {
                f.setIconSize((int) new Size(c.getSize(), SizeUnits.PT).pixels());
            });
        }

        button.setGraphic(getGraphic());
        button.getStyleClass().add("dropdown-comp");

        items.forEach(comp -> {
            var i = new MenuItem(null,comp.createRegion());
            button.getItems().add(i);
        });

        return new SimpleCompStructure<>(button);
    }
}
