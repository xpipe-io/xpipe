package io.xpipe.app.comp.base;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class BackgroundImageComp extends Comp<CompStructure<Pane>> {

    private final Image image;

    public BackgroundImageComp(Image image) {
        this.image = image;
    }

    @Override
    public CompStructure<Pane> createBase() {
        ImageView v = new ImageView(image);
        Pane pane = new Pane(v);
        v.fitWidthProperty().bind(pane.widthProperty());
        v.fitHeightProperty().bind(pane.heightProperty());
        if (image == null) {
            return new SimpleCompStructure<>(pane);
        }

        double imageAspect = image.getWidth() / image.getHeight();
        ChangeListener<? super Number> cl = (c, o, n) -> {
            double paneAspect = pane.getWidth() / pane.getHeight();

            double relViewportWidth;
            double relViewportHeight;

            // Pane width too big for image
            if (paneAspect > imageAspect) {
                relViewportWidth = 1;
                double newImageHeight = pane.getWidth() / imageAspect;
                relViewportHeight = Math.min(1, pane.getHeight() / newImageHeight);
            }

            // Height too big
            else {
                relViewportHeight = 1;
                double newImageWidth = pane.getHeight() * imageAspect;
                relViewportWidth = Math.min(1, pane.getWidth() / newImageWidth);
            }

            v.setViewport(new Rectangle2D(
                    ((1 - relViewportWidth) / 2.0) * image.getWidth(),
                    ((1 - relViewportHeight) / 2.0) * image.getHeight(),
                    image.getWidth() * relViewportWidth,
                    image.getHeight() * relViewportHeight));
        };
        pane.widthProperty().addListener(cl);
        pane.heightProperty().addListener(cl);
        return new SimpleCompStructure<>(pane);
    }
}
