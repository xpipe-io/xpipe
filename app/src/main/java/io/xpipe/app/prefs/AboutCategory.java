package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import atlantafx.base.theme.Styles;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.util.List;

public class AboutCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "about";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2i-information-outline");
    }

    @Override
    protected Comp<?> create() {
        var props = createProperties();
        var update = new UpdateCheckComp().prefWidth(600);
        return new VerticalComp(List.of(
                        props,
                        Comp.vspacer(1),
                        update,
                        Comp.vspacer(5),
                        Comp.hseparator().padding(Insets.EMPTY).maxWidth(600)))
                .apply(s -> s.get().setFillWidth(true))
                .apply(struc -> struc.get().setSpacing(12))
                .styleClass("information")
                .styleClass("about-tab");
    }

    private Comp<?> createProperties() {
        var title = Comp.of(() -> {
            return JfxHelper.createNamedEntry(
                    AppI18n.observable("xPipeClient"),
                    new SimpleStringProperty("Version " + AppProperties.get().getVersion() + " ("
                            + AppProperties.get().getArch() + ")"),
                    "logo/logo.png");
        });

        if (OsType.getLocal() != OsType.MACOS) {
            title.styleClass(Styles.TEXT_BOLD);
        }

        var section = new OptionsBuilder()
                .addComp(Comp.vspacer(40))
                .addComp(title, null)
                .addComp(Comp.vspacer(10))
                .name("build")
                .addComp(new LabelComp(AppProperties.get().getBuild()), null)
                .name("distribution")
                .addComp(new LabelComp(AppDistributionType.get().toTranslatedString()))
                .name("virtualMachine")
                .addComp(
                        new LabelComp(System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name")
                                + " " + System.getProperty("java.vm.version")),
                        null)
                .buildComp();
        return section.styleClass("properties-comp");
    }
}
