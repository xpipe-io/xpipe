package io.xpipe.app.prefs;

import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.util.Hyperlinks;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class ThirdPartyDependencyListComp extends SimpleRegionBuilder {

    private TitledPane createPane(ThirdPartyDependency t) {
        var tp = new TitledPane();
        RegionDescriptor.builder()
                .name(new ReadOnlyStringWrapper(t.name()))
                .build()
                .apply(tp);
        tp.setExpanded(false);
        var link = new Hyperlink(t.name() + " @ " + t.version());
        link.setOnAction(e -> {
            Hyperlinks.open(t.link());
        });
        tp.setPadding(Insets.EMPTY);
        tp.setGraphic(link);
        tp.setAlignment(Pos.CENTER_LEFT);
        AppFontSizes.xs(tp);

        var licenseName = new Label("(" + t.licenseName() + ")");
        var sp = new StackPane(link, licenseName);
        StackPane.setAlignment(licenseName, Pos.CENTER_RIGHT);
        StackPane.setAlignment(link, Pos.CENTER_LEFT);
        sp.prefWidthProperty().bind(tp.widthProperty().subtract(65));
        tp.setGraphic(sp);

        var text = new TextArea();
        text.setEditable(false);
        text.setText(t.licenseText());
        text.setWrapText(true);
        text.setPrefHeight(300);
        text.maxWidthProperty().bind(tp.widthProperty());
        AppFontSizes.xs(text);
        tp.setContent(text);
        AppFontSizes.xs(tp);
        return tp;
    }

    @Override
    public Region createSimple() {
        var tps = ThirdPartyDependency.getAll().stream().map(this::createPane).toArray(TitledPane[]::new);
        var acc = new Accordion(tps);
        acc.getStyleClass().add("third-party-dependency-list-comp");
        acc.setPrefWidth(500);
        var sp = new ScrollPane(acc);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }
}
