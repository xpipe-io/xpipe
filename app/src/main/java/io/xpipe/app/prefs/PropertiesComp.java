package io.xpipe.app.prefs;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.OptionsBuilder;
import javafx.scene.layout.Region;

public class PropertiesComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var title = Comp.of(() -> {
            return JfxHelper.createNamedEntry(AppI18n.get("xPipeClient"), "Version " + AppProperties.get().getVersion() + " ("
                    + AppProperties.get().getArch() + ")", "logo/logo_48x48.png");
        });

        var section = new OptionsBuilder()
                .addComp(title, null)
                .addComp(Comp.vspacer(10))
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
