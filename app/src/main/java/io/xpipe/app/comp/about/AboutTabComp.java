package io.xpipe.app.comp.about;

import io.xpipe.app.util.Hyperlinks;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class AboutTabComp extends Comp<CompStructure<?>> {

    private Region createDepsList() {
        var deps = new ThirdPartyDependencyListComp().createRegion();
        return deps;
    }

    private Comp<?> hyperlink(String link) {
        return Comp.of(() -> {
            var hl = new Hyperlink(link);
            hl.setOnAction(e -> {
                Hyperlinks.open(link);
            });
            hl.setMaxWidth(250);
            return hl;
        });
    }

    private Comp<?> createLinks() {
        return new DynamicOptionsBuilder(false)
                .addTitle("links")
                .addComp(I18n.observable("website"), hyperlink(Hyperlinks.WEBSITE), null)
                .addComp(I18n.observable("documentation"), hyperlink(Hyperlinks.DOCUMENTATION), null)
                .addComp(I18n.observable("discord"), hyperlink(Hyperlinks.DISCORD), null)
                .addComp(I18n.observable("slack"), hyperlink(Hyperlinks.SLACK), null)
                .addComp(I18n.observable("github"), hyperlink(Hyperlinks.GITHUB), null)
                .buildComp();
    }

    private Region createThirdPartyDeps() {
        var label = new Label(I18n.get("openSourceNotices"), new FontIcon("mdi2o-open-source-initiative"));
        label.getStyleClass().add("open-source-header");
        var list = createDepsList();
        var box = new VBox(label, list);
        box.getStyleClass().add("open-source-notices");
        return box;
    }

    @Override
    public CompStructure<?> createBase() {
        var props = new PropertiesComp();
        var update = new UpdateCheckComp();
        var box = new VerticalComp(List.of(props, update, createLinks(), new BrowseDirectoryComp()))
                .apply(s -> s.get().setFillWidth(true))
                .styleClass("information");

        return Comp.derive(box, boxS -> {
                    var bp = new BorderPane();
                    bp.setLeft(boxS);
                    var deps = createThirdPartyDeps();
                    bp.setRight(createThirdPartyDeps());
                    deps.prefWidthProperty().bind(bp.widthProperty().divide(2));
                    boxS.prefWidthProperty().bind(bp.widthProperty().divide(2));
                    bp.getStyleClass().add("about-tab");
                    return bp;
                })
                .createStructure();
    }
}
