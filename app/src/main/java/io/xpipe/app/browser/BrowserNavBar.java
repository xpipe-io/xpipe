package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class BrowserNavBar extends SimpleComp {

    private static final PseudoClass INVISIBLE = PseudoClass.getPseudoClass("invisible");

    private final OpenFileSystemModel model;

    public BrowserNavBar(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var path = new SimpleStringProperty(model.getCurrentPath().get());
        SimpleChangeListener.apply(model.getCurrentPath(), (newValue) -> {
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
                    SimpleChangeListener.apply(struc.get().focusedProperty(), val -> {
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

                    SimpleChangeListener.apply(model.getInOverview(), val -> {
                        // Pseudo classes do not apply if set instantly before shown
                        // If we start a new tab with a directory set, we have to set the pseudo class one pulse later
                        Platform.runLater(() -> {
                            struc.get().pseudoClassStateChanged(INVISIBLE, !val && !struc.get().isFocused());
                        });
                    });

                    struc.get().setPromptText("Overview of " + model.getName());
                })
                .shortcut(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN), s -> {
                    s.get().requestFocus();
                })
                .accessibleText("Current path");

        var graphic = Bindings.createStringBinding(
                () -> {
                    return model.getCurrentDirectory() != null
                            ? FileIconManager.getFileIcon(model.getCurrentDirectory(), false)
                            : "home_icon.svg";
                },
                model.getCurrentPath());
        var breadcrumbsGraphic = PrettyImageHelper.ofSvg(graphic, 16, 16)
                .padding(new Insets(0, 0, 1, 0))
                .styleClass("path-graphic")
                .createRegion();

        var homeButton = new Button(null, breadcrumbsGraphic);
        homeButton.setAccessibleText("Directory options");
        homeButton.getStyleClass().add(Styles.LEFT_PILL);
        homeButton.getStyleClass().add("path-graphic-button");
        new ContextMenuAugment<>(event -> event.getButton() == MouseButton.PRIMARY, () -> {
                    return model.getInOverview().get() ? null : new BrowserContextMenu(model, null);
                })
                .augment(new SimpleCompStructure<>(homeButton));

        var historyButton = new Button(null, new FontIcon("mdi2h-history"));
        historyButton.setAccessibleText("History");
        historyButton.getStyleClass().add(Styles.RIGHT_PILL);
        // historyButton.getStyleClass().add("path-graphic-button");
        new ContextMenuAugment<>(event -> event.getButton() == MouseButton.PRIMARY, this::createContextMenu)
                .augment(new SimpleCompStructure<>(historyButton));

        var breadcrumbs = new BrowserBreadcrumbBar(model).grow(false, true);
        var stack = new StackComp(List.of(pathBar, breadcrumbs))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .hgrow()
                .apply(struc -> {
                    var t = struc.get().getChildren().get(0);
                    var b = struc.get().getChildren().get(1);
                    b.setOnMouseClicked(event -> {
                        t.requestFocus();
                        event.consume();
                    });
                    b.visibleProperty()
                            .bind(Bindings.createBooleanBinding(
                                    () -> {
                                        return !t.isFocused()
                                                && !model.getInOverview().get();
                                    },
                                    t.focusedProperty(),
                                    model.getInOverview()));
                })
                .grow(false, true);

        var topBox = new HorizontalComp(List.of(Comp.of(() -> homeButton), stack, Comp.of(() -> historyButton)))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> {
                    ((Region) struc.get().getChildren().get(0))
                            .minHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                    ((Region) struc.get().getChildren().get(0))
                            .maxHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());

                    ((Region) struc.get().getChildren().get(2))
                            .minHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                    ((Region) struc.get().getChildren().get(2))
                            .maxHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                })
                .apply(struc -> {
                    struc.get().setPickOnBounds(false);
                })
                .hgrow();

        return topBox.createRegion();
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
