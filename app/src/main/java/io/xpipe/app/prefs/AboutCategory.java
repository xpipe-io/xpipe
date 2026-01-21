package io.xpipe.app.prefs;


import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.platform.JfxHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.update.AppDistributionType;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import atlantafx.base.theme.Styles;

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
    protected BaseRegionBuilder<?,?> create() {
        var props = createProperties();
        var update = new UpdateCheckComp().prefWidth(600);
        return new VerticalComp(List.of(
                        props,
                        RegionBuilder.vspacer(1),
                        update,
                        RegionBuilder.vspacer(5),
                        RegionBuilder.hseparator().padding(Insets.EMPTY).maxWidth(600)))
                .apply(s -> s.setFillWidth(true))
                .apply(struc -> struc.setSpacing(12))
                .style("information")
                .style("about-tab");
    }

    private BaseRegionBuilder<?,?> createProperties() {
        var title = RegionBuilder.of(() -> {
            return JfxHelper.createNamedEntry(
                    new ReadOnlyStringWrapper(AppNames.ofCurrent().getName() + " Desktop"),
                    new SimpleStringProperty("Version " + AppProperties.get().getVersion() + " ("
                            + AppProperties.get().getArch() + ")"),
                    "logo/logo.png");
        });

        title.style(Styles.TEXT_BOLD);

        var section = new OptionsBuilder()
                .addComp(RegionBuilder.vspacer(40))
                .addComp(title, null)
                .addComp(RegionBuilder.vspacer(10))
                .name("build")
                .addComp(
                        new LabelComp(AppProperties.get().getBuild())
                                .describe(
                                        d -> d.focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)),
                        null)
                .name("distribution")
                .addComp(new LabelComp(AppDistributionType.get().toTranslatedString())
                        .describe(d -> d.focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)))
                .name("virtualMachine")
                .addComp(
                        new LabelComp(System.getProperty("java.vm.vendor") + " "
                                        + System.getProperty("java.vm.name")
                                        + " "
                                        + System.getProperty("java.vm.version"))
                                .describe(
                                        d -> d.focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)),
                        null)
                .buildComp();
        return section.style("properties-comp");
    }
}
