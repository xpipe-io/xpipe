package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.util.ContextMenuHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.value.ObservableValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DropdownComp extends Comp<CompStructure<Button>> {

    private final List<Comp<?>> items;

    public DropdownComp(List<Comp<?>> items) {
        this.items = items;
    }

    @Override
    public CompStructure<Button> createBase() {
        var cm = ContextMenuHelper.create();
        cm.getItems()
                .setAll(items.stream()
                        .map(comp -> {
                            return new MenuItem(null, comp.createRegion());
                        })
                        .toList());

        Button button = (Button) new ButtonComp(null, () -> {})
                .apply(new ContextMenuAugment<>(e -> true, null, () -> {
                    return cm;
                }))
                .createRegion();

        List<? extends ObservableValue<Boolean>> l = cm.getItems().stream()
                .map(menuItem -> menuItem.getGraphic().visibleProperty())
                .toList();
        button.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> {
                            return l.stream().anyMatch(booleanObservableValue -> booleanObservableValue.getValue());
                        },
                        l.toArray(ObservableValue[]::new)));

        var graphic = new FontIcon("mdi2c-chevron-double-down");
        button.fontProperty().subscribe(c -> {
            graphic.iconSizeProperty().bind(new ReadOnlyIntegerWrapper((int) new Size(c.getSize(), SizeUnits.PT).pixels()));
        });

        button.setGraphic(graphic);
        button.getStyleClass().add("dropdown-comp");
        button.setAccessibleText("Dropdown actions");

        return new SimpleCompStructure<>(button);
    }
}
