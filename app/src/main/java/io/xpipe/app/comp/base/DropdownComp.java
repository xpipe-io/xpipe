package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
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
        ContextMenu cm = new ContextMenu(items.stream()
                .map(comp -> {
                    return new MenuItem(null, comp.createRegion());
                })
                .toArray(MenuItem[]::new));

        Button button = (Button) new ButtonComp(null, () -> {})
                .apply(new ContextMenuAugment<>(e -> true, () -> {
                    return cm;
                }))
                .createRegion();

        button.visibleProperty()
                .bind(BindingsHelper.anyMatch(cm.getItems().stream()
                        .map(menuItem -> menuItem.getGraphic().visibleProperty())
                        .toList()));

        var graphic = new FontIcon("mdi2c-chevron-double-down");
        SimpleChangeListener.apply(button.fontProperty(), c -> {
            graphic.setIconSize((int) new Size(c.getSize(), SizeUnits.PT).pixels());
        });

        button.setGraphic(graphic);
        button.getStyleClass().add("dropdown-comp");
        button.setAccessibleText("Dropdown actions");

        return new SimpleCompStructure<>(button);
    }
}
