package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.LicenseProvider;

public class LinksCategory extends AppPrefsCategory {

    private BaseRegionBuilder<?, ?> createLinks() {
        return new OptionsBuilder()
                .addTitle("links")
                .addComp(RegionBuilder.vspacer(19))
                .addComp(
                        new TileButtonComp("activeLicense", "activeLicenseDescription", "mdi2k-key-outline", e -> {
                                    AppLayoutModel.get().selectLicense();
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .hide(LicenseProvider.get().hasPaidLicense())
                .addComp(
                        new TileButtonComp("discord", "discordDescription", "bi-discord", e -> {
                                    Hyperlinks.open(Hyperlinks.DISCORD);
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .addComp(
                        new TileButtonComp("reddit", "redditDescription", "mdi2r-reddit", e -> {
                                    Hyperlinks.open(Hyperlinks.REDDIT);
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .addComp(
                        new TileButtonComp(
                                        "documentation", "documentationDescription", "mdi2b-book-open-variant", e -> {
                                            Hyperlinks.open(DocumentationLink.getRoot());
                                            e.consume();
                                        })
                                .maxWidth(2000),
                        null)
                .addComp(
                        new TileButtonComp("tryPtb", "tryPtbDescription", "mdoal-insights", e -> {
                                    Hyperlinks.open(Hyperlinks.GITHUB_PTB);
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .addComp(
                        new TileButtonComp("privacy", "privacyDescription", "mdomz-privacy_tip", e -> {
                                    DocumentationLink.PRIVACY.open();
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .addComp(
                        new TileButtonComp("thirdParty", "thirdPartyDescription", "mdi2o-open-source-initiative", e -> {
                                    var comp = new ThirdPartyDependencyListComp()
                                            .prefWidth(650)
                                            .style("open-source-notices");
                                    var modal = ModalOverlay.of("openSourceNotices", comp);
                                    modal.show();
                                })
                                .maxWidth(2000))
                .addComp(
                        new TileButtonComp("eula", "eulaDescription", "mdi2c-card-text-outline", e -> {
                                    DocumentationLink.EULA.open();
                                    e.consume();
                                })
                                .maxWidth(2000),
                        null)
                .addComp(RegionBuilder.vspacer(40))
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
    protected BaseRegionBuilder<?, ?> create() {
        return createLinks().style("information").style("about-tab").apply(struc -> struc.setPrefWidth(600));
    }
}
