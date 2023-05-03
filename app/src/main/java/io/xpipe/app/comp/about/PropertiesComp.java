package io.xpipe.app.comp.about;

import io.xpipe.app.core.App;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.util.DynamicOptionsBuilder;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class PropertiesComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var title = Comp.of(() -> {
            var image = new ImageView(App.getApp().getIcon());
            image.setPreserveRatio(true);
            image.setFitHeight(40);
            var label = new Label(AppI18n.get("xPipeClient"), image);
            label.getStyleClass().add("header");
            AppFont.setSize(label, 5);
            return label;
        });

        var section = new DynamicOptionsBuilder(false)
                .addComp(title, null)
                .addComp(
                        AppI18n.observable("version"),
                        new LabelComp(AppProperties.get().getVersion() + " (" + AppProperties.get().getArch() + ")"),
                        null)
                .addComp(
                        AppI18n.observable("build"),
                        new LabelComp(AppProperties.get().getBuild()),
                        null)
                .addComp(AppI18n.observable("runtimeVersion"), new LabelComp(System.getProperty("java.vm.version")), null)
                .addComp(
                        AppI18n.observable("virtualMachine"),
                        new LabelComp(System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name")),
                        null)
                .buildComp();
        return section.styleClass("properties-comp").createRegion();
    }
}
