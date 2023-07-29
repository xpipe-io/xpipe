package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SideMenuBarComp extends Comp<CompStructure<VBox>> {

    private final Property<AppLayoutModel.Entry> value;
    private final List<AppLayoutModel.Entry> entries;

    public SideMenuBarComp(Property<AppLayoutModel.Entry> value, List<AppLayoutModel.Entry> entries) {
        this.value = value;
        this.entries = entries;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var vbox = new VBox();
        vbox.setFillWidth(true);

        var selected = PseudoClass.getPseudoClass("selected");
        entries.forEach(e -> {
            var b = new IconButtonComp(e.icon(), () -> value.setValue(e)).apply(new FancyTooltipAugment<>(e.name()));
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
                struc.get().pseudoClassStateChanged(selected, value.getValue().equals(e));
                value.addListener((c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        struc.get().pseudoClassStateChanged(selected, n.equals(e));
                    });
                });
            });
            vbox.getChildren().add(b.createRegion());
        });

        {
            var fi = new FontIcon("mdi2u-update");
            var b = new IconButtonComp("mdi2u-update", () -> UpdateAvailableAlert.showIfNeeded());
            b.hide(PlatformThread.sync(Bindings.createBooleanBinding(
                    () -> {
                        return XPipeDistributionType.get()
                                        .getUpdateHandler()
                                        .getPreparedUpdate()
                                        .getValue()
                                == null;
                    },
                    XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate())));
            vbox.getChildren().add(b.createRegion());
        }

        var filler = new Button();
        filler.setDisable(true);
        filler.setMaxHeight(3000);
        vbox.getChildren().add(filler);
        VBox.setVgrow(filler, Priority.ALWAYS);
        filler.prefWidthProperty().bind(((Region) vbox.getChildren().get(0)).widthProperty());

        vbox.getStyleClass().add("sidebar-comp");
        return new SimpleCompStructure<>(vbox);
    }

}
