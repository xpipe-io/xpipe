package io.xpipe.app.browser.file;

import io.xpipe.app.browser.icon.BrowserIconManager;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.comp.base.TooltipAugment;
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
import javafx.scene.shape.Rectangle;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

public class BrowserNavBarComp extends Comp<BrowserNavBarComp.Structure> {

    @Override
    public Structure createBase() {
        var pathBar = createPathBar();

        var graphic = Bindings.createStringBinding(
                () -> {
                    return model.getCurrentDirectory() != null
                            ? BrowserIconManager.getFileIcon(model.getCurrentDirectory())
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
        new TooltipAugment<>("history", new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN))
                .augment(historyButton);

        var breadcrumbs = new BrowserBreadcrumbBar(model);

        var pathRegion = pathBar.createStructure().get();
        var breadcrumbsRegion = breadcrumbs.createRegion();
        breadcrumbsRegion.setOnMouseClicked(event -> {
            pathRegion.requestFocus();
            event.consume();
        });
        breadcrumbsRegion.setFocusTraversable(false);
        breadcrumbsRegion
                .visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> {
                            return !pathRegion.isFocused()
                                    && !model.getInOverview().get();
                        },
                        pathRegion.focusedProperty(),
                        model.getInOverview()));
        var stack = new StackPane(pathRegion, breadcrumbsRegion);
        pathRegion.prefHeightProperty().bind(stack.heightProperty());

        // Prevent overflow
        var clip = new Rectangle();
        clip.widthProperty().bind(stack.widthProperty());
        clip.heightProperty().bind(stack.heightProperty());
        breadcrumbsRegion.setClip(clip);

        stack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stack, Priority.ALWAYS);

        var topBox = new HBox(homeButton, stack, historyButton);
        topBox.setFillHeight(true);
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

    private Comp<CompStructure<TextField>> createPathBar() {
        var path = new SimpleStringProperty();
        model.getCurrentPath().subscribe((newValue) -> {
            path.set(newValue != null ? newValue.toString() : null);
        });
        path.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(model.getBusy(), () -> {
                    var changed = model.cdSyncOrRetry(newValue != null && !newValue.isBlank() ? newValue : null, true);
                    changed.ifPresent(s -> {
                        Platform.runLater(() -> path.set(!s.isBlank() ? s : null));
                    });
                });
            });
        });
        var pathBar =
                new TextFieldComp(path, true).styleClass(Styles.CENTER_PILL).styleClass("path-text");
        pathBar.apply(struc -> {
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
                })
                .accessibleText("Current path");
        return pathBar;
    }

    public record Structure(HBox box, TextField textField, Button historyButton) implements CompStructure<HBox> {

        @Override
        public HBox get() {
            return box;
        }
    }

    private static final PseudoClass INVISIBLE = PseudoClass.getPseudoClass("invisible");

    private final BrowserFileSystemTabModel model;

    public BrowserNavBarComp(BrowserFileSystemTabModel model) {
        this.model = model;
    }

    private ContextMenu createContextMenu() {
        var cm = new ContextMenu();

        var f = model.getHistory().getForwardHistory(8).stream().toList();
        for (int i = f.size() - 1; i >= 0; i--) {
            if (f.get(i) == null) {
                continue;
            }

            var mi = new MenuItem(f.get(i).toString());
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
            var current = new MenuItem(model.getHistory().getCurrent().toString());
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

            var mi = new MenuItem(b.get(i).toString());
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
