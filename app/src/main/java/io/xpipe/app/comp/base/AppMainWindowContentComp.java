package io.xpipe.app.comp.base;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.animation.Animation;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import atlantafx.base.util.Animations;

import java.time.Instant;

public class AppMainWindowContentComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var overlay = AppDialog.getModalOverlay();
        var loaded = AppMainWindow.getLoadedContent();
        var bg = Comp.of(() -> {
            var loadingIcon = new ImageView();
            loadingIcon.setFitWidth(64);
            loadingIcon.setFitHeight(64);

            if (OsType.getLocal() != OsType.LINUX) {
                var anim = Animations.pulse(loadingIcon, 1.1);
                anim.setRate(0.85);
                anim.setCycleCount(Animation.INDEFINITE);
                anim.play();
            }

            // This allows for assigning logos even if AppImages has not been initialized yet
            var dir = "img/logo/";
            AppResources.with(AppResources.XPIPE_MODULE, dir, path -> {
                loadingIcon.setImage(AppImages.loadImage(path.resolve("loading.png")));
            });

            var version = new LabelComp((AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe") + " " + AppProperties.get().getVersion());
            version.apply(struc -> {
                AppFont.setSize(struc.get(), 1);
                struc.get().setOpacity(0.6);
            });

            var text = new LabelComp(AppMainWindow.getLoadingText());
            text.apply(struc -> {
                struc.get().setOpacity(0.8);
            });

            var vbox = new VBox(Comp.vspacer().createRegion(), loadingIcon, Comp.vspacer(19).createRegion(), version.createRegion(), Comp.vspacer().createRegion(), text.createRegion(), Comp.vspacer(20).createRegion());
            vbox.setAlignment(Pos.CENTER);

            var pane = new StackPane(vbox);
            pane.setAlignment(Pos.CENTER);
            pane.getStyleClass().add("background");

            loaded.subscribe(region -> {
                if (region != null) {
                    PlatformThread.runNestedLoopIteration();
                    var started = Instant.now();
                    pane.getChildren().add(region);
                    region.setOpacity(0);
                    PlatformThread.runNestedLoopIteration();
                    var elapsed = java.time.Duration.between(started, Instant.now());
                    var fade = Animations.fadeIn(region, Duration.millis(elapsed.toMillis() / 2.5));
                    fade.play();
                    pane.getChildren().remove(vbox);
                    pane.getStyleClass().remove("background");
                }
            });

            return pane;
        });
        var modal = new ModalOverlayComp(bg, overlay);
        return modal.createRegion();
    }
}
