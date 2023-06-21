package io.xpipe.app.prefs;

import atlantafx.base.controls.Tile;
import io.xpipe.app.core.*;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.util.OptionsBuilder;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class PropertiesComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var title = Comp.of(() -> {
            var image = new ImageView(AppImages.image("logo/logo_48x48.png"));
            image.setPreserveRatio(true);
            image.setSmooth(true);
            image.setFitHeight(40);
            var label = new Label(AppI18n.get("xPipeClient"), image);
            label.getStyleClass().add("header");
            AppFont.setSize(label, 5);
            return new Tile(AppI18n.get("xPipeClient"), "Version " + AppProperties.get().getVersion() + " ("
                    + AppProperties.get().getArch() + ")", image);
        });

        var section = new OptionsBuilder()
                .addComp(title, null)
                .name("build")
                .addComp(
                        new LabelComp(AppProperties.get().getBuild()),
                        null)
                .name("runtimeVersion")
                .addComp(
                        new LabelComp(System.getProperty("java.vm.version")),
                        null)
                .name("virtualMachine")
                .addComp(
                        new LabelComp(System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name")),
                        null)
                .buildComp();
        return section.styleClass("properties-comp").createRegion();
    }
}
