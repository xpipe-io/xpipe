package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import javafx.beans.binding.Bindings;
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

        {
            // vbox.getChildren().add(new Spacer(Orientation.VERTICAL));
            var fi = new FontIcon("mdi2u-update");
            var b = new BigIconButton(AppI18n.observable("update"), fi, () -> UpdateAvailableAlert.showIfNeeded());
            b.apply(GrowAugment.create(true, false));
            b.hide(PlatformThread.sync(Bindings.createBooleanBinding(() -> {
                return XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate().getValue() == null;
            }, XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate())));
            vbox.getChildren().add(b.createRegion());
        }

        vbox.getStyleClass().add("sidebar-comp");
        return new SimpleCompStructure<>(vbox);
    }

    public static record Entry(ObservableValue<String> name, String icon, Comp<?> comp) {}
}
