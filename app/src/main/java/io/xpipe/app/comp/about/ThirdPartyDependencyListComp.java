package io.xpipe.app.comp.about;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class ThirdPartyDependencyListComp extends Comp<CompStructure<?>> {

    private TitledPane createPane(ThirdPartyDependency t) {
        var tp = new TitledPane();
        tp.setExpanded(false);
        var link = new Hyperlink(t.name() + " @ " + t.version());
        link.setOnAction(e -> {
            Hyperlinks.open(t.link());
        });
        tp.setGraphic(link);
        tp.setAlignment(Pos.CENTER_LEFT);
        AppFont.medium(tp);

        var licenseName = new Label("(" + t.licenseName() + ")");
        var sp = new StackPane(link, licenseName);
        StackPane.setAlignment(licenseName, Pos.CENTER_RIGHT);
        StackPane.setAlignment(link, Pos.CENTER_LEFT);
        sp.prefWidthProperty().bind(tp.widthProperty().subtract(40));
        tp.setGraphic(sp);

        var text = new TextArea();
        text.setEditable(false);
        text.setText(t.licenseText());
        text.setWrapText(true);
        text.setPrefHeight(300);
        text.prefWidthProperty().bind(tp.widthProperty());
        AppFont.setSize(text, -4);
        tp.setContent(text);
        AppFont.verySmall(tp);
        return tp;
    }

    @Override
    public CompStructure<?> createBase() {
        var tps = ThirdPartyDependency.getAll().stream().map(this::createPane).toArray(TitledPane[]::new);
        var acc = new Accordion(tps);
        acc.getStyleClass().add("third-party-dependency-list-comp");
        acc.setPrefWidth(500);
        var sp = new ScrollPane(acc);
        sp.setFitToWidth(true);
        return new SimpleCompStructure<>(sp);
    }
}
