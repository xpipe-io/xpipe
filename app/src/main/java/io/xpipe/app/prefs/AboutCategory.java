package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppDistributionType;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import atlantafx.base.theme.Styles;

import java.util.List;

public class AboutCategory extends AppPrefsCategory {

    private Comp<?> createLinks() {
        return new OptionsBuilder()
                .addComp(
                        new TileButtonComp("discord", "discordDescription", "mdi2d-discord", e -> {
                                    Hyperlinks.open(Hyperlinks.DISCORD);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp(
                                        "documentation", "documentationDescription", "mdi2b-book-open-variant", e -> {
                                            Hyperlinks.open(Hyperlinks.DOCS);
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp("tryPtb", "tryPtbDescription", "mdi2t-test-tube", e -> {
                                    Hyperlinks.open(Hyperlinks.GITHUB_PTB);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp("privacy", "privacyDescription", "mdomz-privacy_tip", e -> {
                                    Hyperlinks.open(Hyperlinks.DOCS_PRIVACY);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp("thirdParty", "thirdPartyDescription", "mdi2o-open-source-initiative", e -> {
                                    var comp = new ThirdPartyDependencyListComp()
                                            .prefWidth(650)
                                            .styleClass("open-source-notices");
                                    var modal = ModalOverlay.of("openSourceNotices", comp);
                                    modal.show();
                                })
                                .grow(true, false))
                .addComp(
                        new TileButtonComp("eula", "eulaDescription", "mdi2c-card-text-outline", e -> {
                                    Hyperlinks.open(Hyperlinks.DOCS_EULA);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .buildComp();
    }

    @Override
    protected String getId() {
        return "about";
    }

    @Override
    protected Comp<?> create() {
        var props = createProperties().padding(new Insets(0, 0, 0, 5));
        var update = new UpdateCheckComp().grow(true, false);
        return new VerticalComp(List.of(props, Comp.hseparator(), update, Comp.hseparator(), createLinks()))
                .apply(s -> s.get().setFillWidth(true))
                .apply(struc -> struc.get().setSpacing(15))
                .styleClass("information")
                .styleClass("about-tab")
                .apply(struc -> struc.get().setPrefWidth(600));
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
                .addComp(title, null)
                .addComp(Comp.vspacer(10))
                .name("build")
                .addComp(new LabelComp(AppProperties.get().getBuild()), null)
                .name("distribution")
                .addComp(new LabelComp(AppDistributionType.get().toTranslatedString()))
                .name("runtimeVersion")
                .addComp(new LabelComp(System.getProperty("java.vm.version")), null)
                .name("virtualMachine")
                .addComp(
                        new LabelComp(System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name")),
                        null)
                .buildComp();
        return section.styleClass("properties-comp");
    }
}
