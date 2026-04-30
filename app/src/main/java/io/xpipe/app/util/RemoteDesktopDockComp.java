package io.xpipe.app.util;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteDesktopDockComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var w = RemoteDesktopWindow.get();
        var regionMap = new HashMap<RemoteDesktopDockEntry, Region>();
        w.getProcesses().addListener((ListChangeListener<RemoteDesktopDockEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().forEach(e -> {
                            if (e.isInternal()) {
                                regionMap.put(e, e.getInternal().comp().style("internal-content").build());
                            }
                        });
                    } else if (c.wasRemoved()) {
                        c.getRemoved().forEach(e -> {
                            regionMap.remove(e);
                        });
                    }
                }
            });
        });

        var content = createContent(regionMap);
        var bar = createBar(content);
        var vbox = new VBox(bar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        vbox.getStyleClass().add("remote-desktop-dock");
        vbox.focusWithinProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                var target = vbox.getScene().getRoot().lookup(".button:hover");
                if (target == null || (target.getProperties().get("entry") != null &&
                        target.getProperties().get("entry").equals(w.getSelected().getValue()))) {
                    Platform.runLater(() -> {
                        w.focus();
                    });
                }
            }
        });

        content.focusWithinProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && w.getSelected().get() != null && w.getSelected().get().isInternal()) {
                Platform.runLater(() -> {
                    content.requestFocus();
                });
            }
        });

        return vbox;
    }

    private void fillToolbar(ToolBar bar, List<? extends RemoteDesktopDockEntry> list, ObservableBooleanValue requiresRestart) {
        var w = RemoteDesktopWindow.get();
        bar.getItems().forEach(node -> node.getProperties().clear());
        bar.getItems().clear();
        for (var entry : list) {
            var entryRef = new WeakReference<>(entry);
            var graphic = PrettyImageHelper.ofFixedSizeSquare(entry.getIcon(), 16).style("graphic").build();

            var label = new LabelComp(entry.getName()).build();
            label.setAlignment(Pos.CENTER_LEFT);
            label.setGraphic(graphic);
            label.setGraphicTextGap(6);

            var close = new IconButtonComp("mdi2c-close", () -> {
                var v = entryRef.get();
                if (v != null) {
                    ThreadHelper.runAsync(() -> {
                        w.close(v, true);
                    });
                }
            }).style("close-button")
                    .describe(d -> d.nameKey("close")).build();
            AppFontSizes.xs(close);

            var hbox = new HBox(label, new Spacer(), close);
            hbox.setSpacing(4);
            hbox.setAlignment(Pos.CENTER_LEFT);

            var b = new Button(null, hbox);
            if (entry.getColor() != null) {
                b.getStyleClass().add(entry.getColor().getId());
            } else {
                b.getStyleClass().add("gray");
            }
            b.getStyleClass().add("color-box");
            b.getStyleClass().add("tab-button");
            b.getProperties().put("entry", entry);
            b.setOnAction(event -> {
                var v = entryRef.get();
                if (v != null) {
                    w.select(v);
                }
                event.consume();
            });
            bar.getItems().add(b);
        }

        bar.getItems().add(new Spacer());

        var buttons = new HBox();
        buttons.setFillHeight(true);
        buttons.getStyleClass().add("control-buttons");
        buttons.setSpacing(6);
        bar.getItems().add(buttons);

        var restartButton = new ButtonComp(AppI18n.observable("reloadSizes"), new LabelGraphic.IconGraphic("mdi2r-restart"), () -> {
            w.reconnectResize();
        });
        restartButton.visible(requiresRestart);
        buttons.getChildren().add(restartButton.build());
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

    private Region createBar(Region content) {
        var w = RemoteDesktopWindow.get();
        var bar = new ToolBar();

        var requiresRestart = new SimpleBooleanProperty();
        if (RemoteDesktopWindow.get().supportsDocking()) {
            var ref = new WeakReference<>(content);
            GlobalTimer.scheduleUntil(Duration.ofMillis(200), false, () -> {
                if (ref.get() == null) {
                    return true;
                }

                var rect = w.getDockBounds();
                requiresRestart.set(rect != null && w.getSelected().get() != null && w.getSelected().get().requiresRestart(rect.getW(), rect.getH()));
                return false;
            });
        }

        fillToolbar(bar, w.getProcesses(), requiresRestart);
        w.getProcesses().addListener((ListChangeListener<? super RemoteDesktopDockEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                fillToolbar(bar, c.getList(), requiresRestart);
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

    private Region createContent(Map<RemoteDesktopDockEntry, Region> map) {
        var w = RemoteDesktopWindow.get();

        var stack = new StackPane();

        if (w.supportsDocking()) {
            var dock = new WindowDockComp<>(w.getModel());
            dock.style("dock");
            stack.getChildren().add(dock.build());
        } else {
            var r = new Region();
            r.getStyleClass().add("dock");
            stack.getChildren().add(r);
        }

        w.getSelected().subscribe(entry -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (stack.getChildren().size() > 1) {
                    stack.getChildren().removeLast();
                }

                if (entry != null && entry.isInternal()) {
                    Region r = map.get(entry);
                    stack.getChildren().add(r);
                    r.requestFocus();
                }
            });
        });

        stack.focusedProperty().subscribe(focus -> {
            if (focus && stack.getChildren().size() > 1) {
                stack.getChildren().getLast().requestFocus();
            }
        });

        return stack;
    }
}
