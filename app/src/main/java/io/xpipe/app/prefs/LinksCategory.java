package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;

public class LinksCategory extends AppPrefsCategory {

    private Comp<?> createLinks() {
        return new OptionsBuilder()
                .addTitle("links")
                .addComp(Comp.vspacer(19))
                .addComp(
                        new TileButtonComp("discord", "discordDescription", "bi-discord", e -> {
                                    Hyperlinks.open(Hyperlinks.DISCORD);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp("reddit", "redditDescription", "mdi2r-reddit", e -> {
                                    Hyperlinks.open(Hyperlinks.REDDIT);
                                    e.consume();
                                })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp(
                                        "documentation", "documentationDescription", "mdi2b-book-open-variant", e -> {
                                            Hyperlinks.open(DocumentationLink.getRoot());
                                            e.consume();
                                        })
                                .grow(true, false),
                        null)
                .addComp(
                        new TileButtonComp("tryPtb", "tryPtbDescription", "mdoal-insights", e -> {
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
                .addComp(Comp.vspacer(40))
                .buildComp();
    }

    @Override
    protected String getId() {
        return "links";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2l-link-box-outline");
    }

    @Override
    protected Comp<?> create() {
        return createLinks().styleClass("information").styleClass("about-tab").apply(struc -> struc.get()
                .setPrefWidth(600));
    }
}
