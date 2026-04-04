package io.xpipe.app.util;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class RemoteDesktopDockComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var bar = createBar();
        var content = createContent();
        var vbox = new VBox(bar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        vbox.getStyleClass().add("remote-desktop-dock");
        vbox.focusWithinProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                var w = RemoteDesktopWindow.get();
                var target = vbox.getScene().getRoot().lookup(".button:hover");
                if (target == null || (target.getProperties().get("entry") != null &&
                        target.getProperties().get("entry").equals(w.getSelected().getValue()))) {
                    Platform.runLater(() -> {
                        w.focus();
                    });
                }
            }
        });
        return vbox;
    }

    private void fillToolbar(ToolBar bar, List<? extends RemoteDesktopDockEntry> list) {
        var w = RemoteDesktopWindow.get();
        bar.getItems().clear();
        for (var entry : list) {
            var graphic = PrettyImageHelper.ofFixedSizeSquare(entry.getIcon(), 16).style("graphic").build();

            var label = new LabelComp(entry.getName()).build();
            label.setGraphic(graphic);

            var close = new IconButtonComp("mdi2c-close", () -> {
                w.close(entry);
            }).style("close-button")
                    .describe(d -> d.nameKey("close")).build();
            AppFontSizes.sm(close);

            var hbox = new HBox(label, close);
            hbox.setSpacing(6);
            hbox.setAlignment(Pos.CENTER_LEFT);

            var b = new Button(null, hbox);
            if (entry.getColor() != null) {
                b.getStyleClass().add(entry.getColor().getId());
            }
            b.getStyleClass().add("color-box");
            b.getStyleClass().add("tab-button");
            b.getProperties().put("entry", entry);
            b.setOnAction(event -> {
                w.select(entry);
                event.consume();
            });
            bar.getItems().add(b);
        }

        bar.getItems().add(new Spacer());
        var lockIcon = Bindings.createObjectBinding(() -> {
            return new LabelGraphic.IconGraphic(w.getLocked().get() ? "mdi2l-lock-outline" : "mdi2l-lock-open-variant-outline");
        }, w.getLocked());
        var lock = new IconButtonComp(lockIcon, () -> {
            w.toggleLock();
            w.focus();
        }).describe(d -> d.nameKey("toggleSizeLock").showTooltips(true)).style("lock-button").build();
        bar.getItems().add(lock);
    }

    private void updateSelection(ToolBar bar, RemoteDesktopDockEntry entry) {
        for (Node item : bar.getItems()) {
            if (item.getProperties().get("entry") != null) {
                if (item.getProperties().get("entry").equals(entry)) {
                    item.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
                } else {
                    item.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
                }
            }
        }
    }

    private Region createBar() {
        var w = RemoteDesktopWindow.get();
        var bar = new ToolBar();

        fillToolbar(bar, w.getProcesses());
        w.getProcesses().addListener((ListChangeListener<? super RemoteDesktopDockEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                fillToolbar(bar, c.getList());
                updateSelection(bar, w.getSelected().get());
            });
        });

        updateSelection(bar, w.getSelected().getValue());
        w.getSelected().addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                updateSelection(bar, newValue);
            });
        });

        return bar;
    }

    private Region createContent() {
        var w = RemoteDesktopWindow.get();
        var sp = new WindowDockComp<>(w.getModel());
        sp.style("content");
        return sp.build();
    }
}
