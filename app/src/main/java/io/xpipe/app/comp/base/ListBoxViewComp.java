package io.xpipe.app.comp.base;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.PlatformThread;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ListBoxViewComp<T> extends Comp<CompStructure<ScrollPane>> {

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even");
    private static final PseudoClass FIRST = PseudoClass.getPseudoClass("first");
    private static final PseudoClass LAST = PseudoClass.getPseudoClass("last");

    private final ObservableList<T> shown;
    private final ObservableList<T> all;
    private final Function<T, Comp<?>> compFunction;
    private final boolean scrollBar;

    @Setter
    private boolean visibilityControl = false;

    public ListBoxViewComp(
            ObservableList<T> shown, ObservableList<T> all, Function<T, Comp<?>> compFunction, boolean scrollBar) {
        this.shown = shown;
        this.all = all;
        this.compFunction = compFunction;
        this.scrollBar = scrollBar;
    }

    @Override
    public CompStructure<ScrollPane> createBase() {
        Map<T, Region> cache = new IdentityHashMap<>();

        VBox vbox = new VBox();
        vbox.getStyleClass().add("list-box-content");
        vbox.setFocusTraversable(false);
        var scroll = new ScrollPane(vbox);

        refresh(scroll, vbox, shown, all, cache, false);

        var hadScene = new AtomicBoolean(false);
        scroll.sceneProperty().subscribe(scene -> {
            if (scene != null) {
                hadScene.set(true);
                refresh(scroll, vbox, shown, all, cache, true);
            }
        });

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            Platform.runLater(() -> {
                if (scroll.getScene() == null && hadScene.get()) {
                    return;
                }

                refresh(scroll, vbox, c.getList(), all, cache, true);
            });
        });

        if (scrollBar) {
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            scroll.skinProperty().subscribe(newValue -> {
                if (newValue != null) {
                    ScrollBar bar = (ScrollBar) scroll.lookup(".scroll-bar:vertical");
                    bar.opacityProperty()
                            .bind(Bindings.createDoubleBinding(
                                    () -> {
                                        var v = bar.getVisibleAmount();
                                        // Check for rounding and accuracy issues
                                        // It might not be exactly equal to 1.0
                                        return v < 0.99 ? 1.0 : 0.0;
                                    },
                                    bar.visibleAmountProperty()));
                }
            });
        } else {
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setFitToHeight(true);
        }
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("list-box-view-comp");

        registerVisibilityListeners(scroll, vbox);

        return new SimpleCompStructure<>(scroll);
    }

    private void registerVisibilityListeners(ScrollPane scroll, VBox vbox) {
        if (!visibilityControl) {
            return;
        }

        var dirty = new SimpleBooleanProperty();
        var animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!dirty.get()) {
                    return;
                }

                updateVisibilities(scroll, vbox);
                dirty.set(false);
            }
        };

        scroll.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            dirty.set(true);
        });
        scroll.heightProperty().addListener((observable, oldValue, newValue) -> {
            dirty.set(true);
        });
        vbox.heightProperty().addListener((observable, oldValue, newValue) -> {
            dirty.set(true);
        });

        // We can't directly listen to any parent element changing visibility, so this is a compromise
        if (AppLayoutModel.get() != null) {
            AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
                dirty.set(true);
            });
        }
        BrowserFullSessionModel.DEFAULT.getSelectedEntry().addListener((observable, oldValue, newValue) -> {
            dirty.set(true);
        });
        if (StoreViewState.get() != null) {
            StoreViewState.get().getSortMode().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    dirty.set(true);
                });
            });
        }

        vbox.sceneProperty().addListener((observable, oldValue, newValue) -> {
            dirty.set(true);

            if (newValue != null) {
                animationTimer.start();
            } else {
                animationTimer.stop();
            }

            Node c = vbox;
            do {
                c.boundsInParentProperty().addListener((change, oldBounds,newBounds) -> {
                    dirty.set(true);
                });
                // Don't listen to root node changes, that seemingly can cause exceptions
            } while ((c = c.getParent()) != null && c.getParent() != null);

            if (newValue != null) {
                newValue.heightProperty().addListener((observable1, oldValue1, newValue1) -> {
                    dirty.set(true);
                });
            }
        });
    }

    private boolean isVisible(ScrollPane pane, VBox box, Node node) {
        if (pane.getScene() == null || box.getScene() == null || node.getScene() == null) {
            return false;
        }

        var paneHeight = pane.getHeight();
        var scrollCenter = box.getBoundsInLocal().getHeight() * pane.getVvalue();
        var minBoundsHeight = scrollCenter - paneHeight;
        var maxBoundsHeight = scrollCenter + paneHeight;

        var nodeMinHeight = node.getBoundsInParent().getMinY();
        var nodeMaxHeight = node.getBoundsInParent().getMaxY();

        if (paneHeight == 0.0
                || box.getHeight() == 0.0
                || ((Region) node).getHeight() == 0.0
                || nodeMinHeight == nodeMaxHeight) {
            return false;
        }

        if (nodeMaxHeight < minBoundsHeight) {
            return false;
        }

        if (nodeMinHeight > maxBoundsHeight) {
            return false;
        }

        if (pane.getScene().getHeight() > 200) {
            var sceneNodeBounds = node.localToScene(node.getBoundsInLocal());
            if (sceneNodeBounds.getMaxY() < 0
                    || sceneNodeBounds.getMinY() > pane.getScene().getHeight()) {
                return false;
            }
        }

        return true;
    }

    private void updateVisibilities(ScrollPane scroll, VBox vbox) {
        if (!visibilityControl) {
            return;
        }

        int count = 0;
        for (Node child : vbox.getChildren()) {
            var v = isVisible(scroll, vbox, child);
            child.setVisible(v);
            if (v) {
                count++;
            }
        }
        if (count > 10) {
            // System.out.println("Visible: " + count);
        }
    }

    private void refresh(
            ScrollPane scroll,
            VBox listView,
            List<? extends T> shown,
            List<? extends T> all,
            Map<T, Region> cache,
            boolean refreshVisibilities) {
        Runnable update = () -> {
            synchronized (cache) {
                var set = new HashSet<T>();
                // These lists might diverge on updates
                set.addAll(shown);
                set.addAll(all);
                // Clear cache of unused values
                cache.keySet().removeIf(t -> !set.contains(t));
            }

            // Create copy to reduce chances of concurrent modification
            var shownCopy = new ArrayList<>(shown);
            var newShown = shownCopy.stream()
                    .map(v -> {
                        if (!cache.containsKey(v)) {
                            var comp = compFunction.apply(v);
                            if (comp != null) {
                                var r = comp.createRegion();
                                if (visibilityControl) {
                                    r.setVisible(false);
                                }
                                cache.put(v, r);
                            } else {
                                cache.put(v, null);
                            }
                        }

                        return cache.get(v);
                    })
                    .filter(region -> region != null)
                    .toList();

            if (listView.getChildren().equals(newShown)) {
                return;
            }

            for (int i = 0; i < newShown.size(); i++) {
                var r = newShown.get(i);
                r.pseudoClassStateChanged(ODD, i % 2 != 0);
                r.pseudoClassStateChanged(EVEN, i % 2 == 0);
                r.pseudoClassStateChanged(FIRST, i == 0);
                r.pseudoClassStateChanged(LAST, i == newShown.size() - 1);
            }

            var d = new DerivedObservableList<>(listView.getChildren(), true);
            d.setContent(newShown);
            if (refreshVisibilities) {
                updateVisibilities(scroll, listView);
            }
        };
        update.run();
    }
}
