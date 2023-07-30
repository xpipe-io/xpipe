package io.xpipe.app.util;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class JfxHelper {

    public static void fontSize(Node node, double relativeSize) {
        node.setStyle(node.getStyle() + "-fx-font-size: " + (relativeSize) + "em;");
    }

    public static Region createNamedEntry(String nameString, String descString) {
        var header = new Label(nameString);
        AppFont.header(header);
        var desc = new Label(descString);
        AppFont.small(desc);
        var text = new VBox(header, desc);
        text.setSpacing(2);
        return text;
    }

    public static Region createNamedEntry(String nameString, String descString, FontIcon graphic) {
        var header = new Label(nameString);
        var desc = new Label(descString);
        AppFont.small(desc);
        desc.setOpacity(0.65);
        var text = new VBox(header, desc);
        text.setSpacing(2);

        var pane = new StackPane(graphic);
        var hbox = new HBox(pane, text);
        hbox.setSpacing(8);
        pane.prefWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> (header.getHeight() + desc.getHeight()) * 0.6,
                        header.heightProperty(),
                        desc.heightProperty()));
        pane.prefHeightProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> header.getHeight() + desc.getHeight() + 2,
                        header.heightProperty(),
                        desc.heightProperty()));
        pane.prefHeightProperty().addListener((c, o, n) -> {
            var size = Math.min(n.intValue(), 100);
            graphic.setIconSize((int) (size * 0.55));
        });
        return hbox;
    }

    public static Region createNamedEntry(String nameString, String descString, String image) {
        var header = new Label(nameString);
        AppFont.header(header);
        var desc = new Label(descString);
        AppFont.small(desc);
        var text = new VBox(header, desc);
        text.setSpacing(2);

        if (image == null) {
            return text;
        }

        var size = AppFont.getPixelSize(1) + AppFont.getPixelSize(-2) + 8;
        var graphic = new PrettyImageComp(new SimpleStringProperty(image), size, size).createRegion();

        var hbox = new HBox(graphic, text);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(10);

        //        graphic.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> header.getHeight() +
        // desc.getHeight() + 2,
        //                header.heightProperty(), desc.heightProperty()));
        //        graphic.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> header.getHeight() +
        // desc.getHeight() + 2,
        //                header.heightProperty(), desc.heightProperty()));

        return hbox;
    }
}
