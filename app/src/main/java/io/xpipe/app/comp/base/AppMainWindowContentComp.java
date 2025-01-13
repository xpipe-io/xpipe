package io.xpipe.app.comp.base;

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
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import atlantafx.base.util.Animations;

public class AppMainWindowContentComp extends SimpleComp {

    private final Stage stage;

    public AppMainWindowContentComp(Stage stage) {
        this.stage = stage;
    }

    @Override
    protected Region createSimple() {
        var overlay = AppDialog.getModalOverlay();
        var loaded = AppMainWindow.getLoadedContent();
        var bg = Comp.of(() -> {
            var loadingIcon = new ImageView();
            loadingIcon.setFitWidth(64);
            loadingIcon.setFitHeight(64);

            var anim = Animations.pulse(loadingIcon, 1.1);
            if (OsType.getLocal() != OsType.LINUX) {
                anim.setRate(0.85);
                anim.setCycleCount(Animation.INDEFINITE);
                anim.play();
            }

            // This allows for assigning logos even if AppImages has not been initialized yet
            var dir = "img/logo/";
            AppResources.with(AppResources.XPIPE_MODULE, dir, path -> {
                loadingIcon.setImage(AppImages.loadImage(path.resolve("loading.png")));
            });

            var version = new LabelComp((AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe") + " "
                    + AppProperties.get().getVersion());
            version.apply(struc -> {
                AppFont.setSize(struc.get(), 1);
                struc.get().setOpacity(0.6);
            });

            var text = new LabelComp(AppMainWindow.getLoadingText());
            text.apply(struc -> {
                struc.get().setOpacity(0.8);
            });

            var vbox = new VBox(
                    Comp.vspacer().createRegion(),
                    loadingIcon,
                    Comp.vspacer(19).createRegion(),
                    version.createRegion(),
                    Comp.vspacer().createRegion(),
                    text.createRegion(),
                    Comp.vspacer(20).createRegion());
            vbox.setAlignment(Pos.CENTER);

            var pane = new StackPane(vbox);
            pane.setAlignment(Pos.CENTER);
            pane.getStyleClass().add("background");

            loaded.subscribe(struc -> {
                if (struc != null) {
                    PlatformThread.runNestedLoopIteration();
                    struc.prepareAddition();
                    anim.stop();
                    pane.getChildren().add(struc.get());
                    struc.show();
                    pane.getChildren().remove(vbox);
                    pane.getStyleClass().remove("background");
                }
            });

            overlay.addListener((ListChangeListener<? super ModalOverlay>) c -> {
                if (c.next() && c.wasAdded()) {
                    stage.requestFocus();

                    // Close blocking modal windows
                    var childWindows = Window.getWindows().stream()
                            .filter(window -> window instanceof Stage s && stage.equals(s.getOwner()))
                            .toList();
                    childWindows.forEach(window -> {
                        ((Stage) window).close();
                    });
                }
            });

            loaded.addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    stage.requestFocus();
                }
            });

            return pane;
        });
        var modal = new ModalOverlayStackComp(bg, overlay);
        return modal.createRegion();
    }
}
