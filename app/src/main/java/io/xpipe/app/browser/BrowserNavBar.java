package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.file.BrowserContextMenu;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

public class BrowserNavBar extends Comp<BrowserNavBar.Structure> {

    @Override
    public Structure createBase() {
        var path = new SimpleStringProperty(model.getCurrentPath().get());
        model.getCurrentPath().subscribe((newValue) -> {
            path.set(newValue);
        });
        path.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getBusy(), () -> {
                    var changed = model.cdSyncOrRetry(newValue, true);
                    changed.ifPresent(s -> Platform.runLater(() -> path.set(s)));
                });
            });
        });

        var pathBar = new TextFieldComp(path, true)
                .styleClass(Styles.CENTER_PILL)
                .styleClass("path-text")
                .apply(struc -> {
                    struc.get().focusedProperty().subscribe(val -> {
                        struc.get()
                                .pseudoClassStateChanged(
                                        INVISIBLE,
                                        !val && !model.getInOverview().get());

                        if (val) {
                            Platform.runLater(() -> {
                                struc.get().end();
                            });
                        }
                    });

                    model.getInOverview().subscribe(val -> {
                        // Pseudo classes do not apply if set instantly before shown
                        // If we start a new tab with a directory set, we have to set the pseudo class one pulse later
                        Platform.runLater(() -> {
                            struc.get()
                                    .pseudoClassStateChanged(
                                            INVISIBLE, !val && !struc.get().isFocused());
                        });
                    });

                    struc.get().setPromptText("Overview of " + model.getName());
                })
                .accessibleText("Current path");

        var graphic = Bindings.createStringBinding(
                () -> {
                    return model.getCurrentDirectory() != null
                            ? FileIconManager.getFileIcon(model.getCurrentDirectory(), false)
                            : null;
                },
                model.getCurrentPath());
        var breadcrumbsGraphic = PrettyImageHelper.ofFixedSize(graphic, 24, 24)
                .styleClass("path-graphic")
                .createRegion();

        var homeButton = new Button(null, breadcrumbsGraphic);
        homeButton.setAccessibleText("Directory options");
        homeButton.getStyleClass().add(Styles.LEFT_PILL);
        homeButton.getStyleClass().add("path-graphic-button");
        new ContextMenuAugment<>(event -> event.getButton() == MouseButton.PRIMARY, null, () -> {
            return model.getInOverview().get() ? null : new BrowserContextMenu(model, null, false);
        })
                .augment(new SimpleCompStructure<>(homeButton));

        var historyButton = new Button(null, new FontIcon("mdi2h-history"));
        historyButton.setAccessibleText("History");
        historyButton.getStyleClass().add(Styles.RIGHT_PILL);
        new ContextMenuAugment<>(event -> event.getButton() == MouseButton.PRIMARY, null, this::createContextMenu)
                .augment(new SimpleCompStructure<>(historyButton));
        new TooltipAugment<>("history", new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN)).augment(historyButton);

        var breadcrumbs = new BrowserBreadcrumbBar(model).grow(false, true);

        var pathRegion = pathBar.createStructure().get();
        var breadcrumbsRegion = breadcrumbs.createRegion();
        breadcrumbsRegion.setOnMouseClicked(event -> {
            pathRegion.requestFocus();
            event.consume();
        });
        breadcrumbsRegion.setFocusTraversable(false);
        breadcrumbsRegion.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> {
                            return !pathRegion.isFocused()
                                    && !model.getInOverview().get();
                        },
                        pathRegion.focusedProperty(),
                        model.getInOverview()));
        var stack = new StackPane(pathRegion, breadcrumbsRegion);
        stack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stack, Priority.ALWAYS);

        var topBox = new HBox(homeButton, stack, historyButton);
        topBox.setAlignment(Pos.CENTER);
        homeButton.minWidthProperty().bind(pathRegion.heightProperty());
        homeButton.maxWidthProperty().bind(pathRegion.heightProperty());
        homeButton.minHeightProperty().bind(pathRegion.heightProperty());
        homeButton.maxHeightProperty().bind(pathRegion.heightProperty());
        historyButton.minHeightProperty().bind(pathRegion.heightProperty());
        historyButton.maxHeightProperty().bind(pathRegion.heightProperty());
        topBox.setPickOnBounds(false);
        HBox.setHgrow(topBox, Priority.ALWAYS);

        return new Structure(topBox, pathRegion, historyButton);
    }

    public record Structure(HBox box, TextField textField, Button historyButton) implements CompStructure<HBox> {

        @Override
        public HBox get() {
            return box;
        }
    }

    private static final PseudoClass INVISIBLE = PseudoClass.getPseudoClass("invisible");

    private final OpenFileSystemModel model;

    public BrowserNavBar(OpenFileSystemModel model) {
        this.model = model;
    }

    private ContextMenu createContextMenu() {
        if (model.getCurrentDirectory() == null) {
            return null;
        }

        var cm = new ContextMenu();

        var f = model.getHistory().getForwardHistory(8).stream().toList();
        for (int i = f.size() - 1; i >= 0; i--) {
            if (f.get(i) == null) {
                continue;
            }

            var mi = new MenuItem(f.get(i));
            int target = i + 1;
            mi.setOnAction(event -> {
                ThreadHelper.runFailableAsync(() -> {
                    BooleanScope.executeExclusive(model.getBusy(), () -> {
                        model.forthSync(target);
                    });
                });
                event.consume();
            });
            cm.getItems().add(mi);
        }
        if (!f.isEmpty()) {
            cm.getItems().add(new SeparatorMenuItem());
        }

        if (model.getHistory().getCurrent() != null) {
            var current = new MenuItem(model.getHistory().getCurrent());
            current.setDisable(true);
            cm.getItems().add(current);
        }

        var b = model.getHistory().getBackwardHistory(Integer.MAX_VALUE).stream()
                .toList();
        if (!b.isEmpty()) {
            cm.getItems().add(new SeparatorMenuItem());
        }
        for (int i = 0; i < b.size(); i++) {
            if (b.get(i) == null) {
                continue;
            }

            var mi = new MenuItem(b.get(i));
            int target = i + 1;
            mi.setOnAction(event -> {
                ThreadHelper.runFailableAsync(() -> {
                    BooleanScope.executeExclusive(model.getBusy(), () -> {
                        model.backSync(target);
                    });
                });
                event.consume();
            });
            cm.getItems().add(mi);
        }

        cm.addEventHandler(Menu.ON_SHOWING, e -> {
            Node content = cm.getSkin().getNode();
            if (content instanceof Region r) {
                r.setMaxHeight(600);
            }
        });
        return cm;
    }
}
