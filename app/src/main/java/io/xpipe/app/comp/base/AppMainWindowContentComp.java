package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.*;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.animation.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
        var overlay = AppDialog.getModalOverlays();
        var loaded = AppMainWindow.getInstance().getLoadedContent();
        var sidebarPresent = new SimpleBooleanProperty();
        var bg = Comp.of(() -> {
            var loadingIcon = new ImageView();
            loadingIcon.setFitWidth(128);
            loadingIcon.setFitHeight(128);

            var color = AppPrefs.get() != null && AppPrefs.get().theme().getValue().isDark() ?
                    Color.web("#0b898aff") : Color.web("#0b898aff");
            DropShadow shadow = new DropShadow();
            shadow.setRadius(10);
            shadow.setColor(color);

            var loadingAnimation = new AnimationTimer() {

                long offset;

                @Override
                public void handle(long now) {
                    // Increment offset as we are always having 60fps
                    // Prevents animation jumps when the animation timer isn't called for a long time
                    offset += 1000 / 60;

                    // Move shadow in a circle
                    var rad = -(offset % 1000.0) / 1000.0 * 2 * Math.PI;
                    var x = Math.sin(rad);
                    var y = Math.cos(rad);
                    shadow.setOffsetX(x * 3);
                    shadow.setOffsetY(y * 3);
                }
            };

            loadingIcon.setEffect(shadow);
            loadingAnimation.start();

            // This allows for assigning logos even if AppImages has not been initialized yet
            var dir = "img/logo/";
            AppResources.with(AppResources.XPIPE_MODULE, dir, path -> {
                var image = AppPrefs.get() != null && AppPrefs.get().theme().getValue().isDark() ?
                        path.resolve("loading-dark.png") : path.resolve("loading.png");
                loadingIcon.setImage(AppImages.loadImage(image));
            });

            var version = new LabelComp((AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe") + " "
                    + AppProperties.get().getVersion());
            version.apply(struc -> {
                AppFontSizes.apply(struc.get(), appFontSizes -> "15");
                struc.get().setOpacity(0.65);
            });

            var text = new LabelComp(AppMainWindow.getInstance().getLoadingText());
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
            pane.setAlignment(Pos.TOP_LEFT);
            pane.getStyleClass().add("background");

            loaded.subscribe(struc -> {
                if (struc != null) {
                    TrackEvent.info("Window content node set");
                    PlatformThread.runNestedLoopIteration();
                    struc.prepareAddition();
                    pane.getChildren().add(struc.get());
                    sidebarPresent.set(true);
                    PlatformThread.runNestedLoopIteration();
                    pane.getStyleClass().remove("background");
                    pane.getChildren().remove(vbox);
                    loadingAnimation.stop();
                    struc.show();
                    TrackEvent.info("Window content node shown");
                }
            });

            overlay.addListener((ListChangeListener<? super ModalOverlay>) c -> {
                if (c.next() && c.wasAdded()) {
                    AppMainWindow.getInstance().focus();

                    // Close blocking modal windows
                    var childWindows = Window.getWindows().stream()
                            .filter(window -> window instanceof Stage s && stage.equals(s.getOwner()))
                            .toList();
                    childWindows.forEach(window -> {
                        ((Stage) window).close();
                    });
                }
            });

            return pane;
        });

        var modal = new ModalOverlayStackComp(bg, overlay);
        var r =  modal.createRegion();
        var p = r.lookupAll(".modal-overlay-stack-element");
        sidebarPresent.subscribe(v -> {
           if (v) {
               p.forEach(node -> {
                   node.pseudoClassStateChanged(PseudoClass.getPseudoClass("loaded"), true);
               });
           }
        });

        return r;
    }
}
