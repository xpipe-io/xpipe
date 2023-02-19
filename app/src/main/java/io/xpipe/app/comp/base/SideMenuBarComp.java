package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SideMenuBarComp extends Comp<CompStructure<VBox>> {

    private final Property<SideMenuBarComp.Entry> value;
    private final List<Entry> entries;

    public SideMenuBarComp(Property<Entry> value, List<Entry> entries) {
        this.value = value;
        this.entries = entries;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var vbox = new VBox();
        vbox.setFillWidth(true);

        var selected = PseudoClass.getPseudoClass("selected");
        entries.forEach(e -> {
            var fi = new FontIcon(e.icon());
            var b = new BigIconButton(e.name(), fi, () -> value.setValue(e));
            b.apply(GrowAugment.create(true, false));
            b.apply(struc -> {
                struc.get().pseudoClassStateChanged(selected, value.getValue().equals(e));
                value.addListener((c, o, n) -> {
                    struc.get().pseudoClassStateChanged(selected, n.equals(e));
                });
            });
            vbox.getChildren().add(b.createRegion());
        });
        vbox.getStyleClass().add("sidebar-comp");
        return new SimpleCompStructure<>(vbox);
    }

    public static record Entry(ObservableValue<String> name, String icon, Comp<?> comp) {}
}
