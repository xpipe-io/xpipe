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
import javafx.beans.property.SimpleBooleanProperty;
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

    private void fillToolbar(Region content, ToolBar bar, List<? extends RemoteDesktopDockEntry> list) {
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
            } else {
                b.getStyleClass().add("gray");
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

        var buttons = new HBox();
        buttons.getStyleClass().add("buttons");
        buttons.setSpacing(6);
        bar.getItems().add(buttons);

        var requiresRestart = new SimpleBooleanProperty();
        var restartButton = new ButtonComp(AppI18n.observable("reloadSizes"), new LabelGraphic.IconGraphic("mdi2r-restart"), () -> {
            w.getRestartTriggered().set(true);
            GlobalTimer.delay(() -> {
                w.getRestartTriggered().set(false);
            }, Duration.ofSeconds(15));

            var rect = w.getDockBounds();
            var toRestart = w.getProcesses().stream().filter(e -> e.requiresRestart(rect.getW(), rect.getH())).toList();

            for (RemoteDesktopDockEntry e : toRestart) {
            ThreadHelper.runFailableAsync(() -> {
                    w.close(e);
                    e.getEntry().getProvider().launch(e.getEntry()).run();
            });
            }
        });
        var ref = new WeakReference<>(content);
        GlobalTimer.scheduleUntil(Duration.ofMillis(200), false, () -> {
            if (ref.get() == null) {
                return true;
            }

            var rect = w.getDockBounds();
            requiresRestart.set(w.getProcesses().stream().anyMatch(e -> e.requiresRestart(rect.getW(), rect.getH())));
            return false;
        });
        restartButton.show(requiresRestart);
        buttons.getChildren().add(restartButton.build());

        var lockText = Bindings.createStringBinding(() -> {
            return w.getLocked().get() ? AppI18n.get("sizeLocked") : AppI18n.get("sizeUnlocked");
        }, w.getLocked(), AppI18n.activeLanguage());
        var lockIcon = Bindings.<LabelGraphic>createObjectBinding(() -> {
            return new LabelGraphic.IconGraphic(w.getLocked().get() ? "mdi2l-lock-outline" : "mdi2l-lock-open-variant-outline");
        }, w.getLocked());
        var lock = new ButtonComp(lockText, lockIcon, () -> {
            w.toggleLock();
            w.focus();
        }).describe(d -> d.nameKey("toggleSizeLock").showTooltips(true)).style("lock-button").build();
        buttons.getChildren().add(lock);
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

        fillToolbar(content, bar, w.getProcesses());
        w.getProcesses().addListener((ListChangeListener<? super RemoteDesktopDockEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                fillToolbar(content, bar, c.getList());
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
            stack.getChildren().add(new Region());
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
