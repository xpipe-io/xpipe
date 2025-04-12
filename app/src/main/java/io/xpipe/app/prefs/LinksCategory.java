package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppDistributionType;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;

import java.util.List;

public class LinksCategory extends AppPrefsCategory {

    private Comp<?> createLinks() {
        return new OptionsBuilder()
                .addTitle("links")
                .addComp(Comp.vspacer(19))
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
                                    DocumentationLink.PRIVACY.open();
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
                                    DocumentationLink.EULA.open();
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(Comp.vspacer(25))
                .buildComp();
    }

    @Override
    protected String getId() {
        return "links";
    }

    @Override
    protected Comp<?> create() {
        return createLinks()
                .styleClass("information")
                .styleClass("about-tab")
                .apply(struc -> struc.get().setPrefWidth(600));
    }
}
