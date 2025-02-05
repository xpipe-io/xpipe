package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.PlatformThread;

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
    private int platformPauseInterval = -1;

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

        refresh(scroll, vbox, shown, all, cache, false, false);

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            refresh(scroll, vbox, c.getList(), all, cache, true, true);
        });

        all.addListener((ListChangeListener<? super T>) c -> {
            synchronized (cache) {
                cache.keySet().retainAll(c.getList());
            }
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

        scroll.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            updateVisibilities(scroll, vbox);
        });
        scroll.sceneProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                updateVisibilities(scroll, vbox);
            });
            if (newValue != null) {
                newValue.heightProperty().addListener((observable1, oldValue1, newValue1) -> {
                    updateVisibilities(scroll, vbox);
                });
            }
        });
        scroll.heightProperty().addListener((observable, oldValue, newValue) -> {
            updateVisibilities(scroll, vbox);
        });
        vbox.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                updateVisibilities(scroll, vbox);
            });
        });

        return new SimpleCompStructure<>(scroll);
    }

    private boolean isVisible(ScrollPane pane, VBox box, Node node) {
        Node c = pane;
        while ( (c = c.getParent()) != null) {
            if (!c.isVisible()) {
                return false;
            }
        }

        var paneHeight = pane.getHeight();
        var scrollCenter = box.getBoundsInLocal().getHeight() * pane.getVvalue();
        var minBoundsHeight = scrollCenter - paneHeight;
        var maxBoundsHeight = scrollCenter + paneHeight;

        var nodeMinHeight = node.getBoundsInParent().getMinY();
        var nodeMaxHeight = node.getBoundsInParent().getMaxY();

        if (nodeMaxHeight < minBoundsHeight) {
            return false;
        }

        if (nodeMinHeight > maxBoundsHeight) {
            return false;
        }

        if (pane.getScene() != null) {
            var sceneBounds = pane.localToScene(pane.getBoundsInLocal());
            var sceneNodeBounds = node.localToScene(node.getBoundsInLocal());
            if (sceneNodeBounds.getMaxY() < 0 || sceneNodeBounds.getMinY() > pane.getScene().getHeight()) {
                return false;
            }
        }

        return true;
    }

    private void updateVisibilities(ScrollPane scroll, VBox vbox) {
        for (Node child : vbox.getChildren()) {
            child.setVisible(isVisible(scroll, vbox, child));
        }
    }

    private void refresh(
            ScrollPane scroll, VBox listView, List<? extends T> shown, List<? extends T> all, Map<T, Region> cache, boolean asynchronous, boolean refreshVisibilities) {
        Runnable update = () -> {
            synchronized (cache) {
                var set = new HashSet<T>();
                // These lists might diverge on updates
                set.addAll(shown);
                set.addAll(all);
                // Clear cache of unused values
                cache.keySet().removeIf(t -> !set.contains(t));
            }

            final long[] lastPause = {System.currentTimeMillis()};
            // Create copy to reduce chances of concurrent modification
            var shownCopy = new ArrayList<>(shown);
            var newShown = shownCopy.stream()
                    .map(v -> {
                        var elapsed = System.currentTimeMillis() - lastPause[0];
                        if (platformPauseInterval != -1 && elapsed > platformPauseInterval) {
                            PlatformThread.runNestedLoopIteration();
                            lastPause[0] = System.currentTimeMillis();
                        }

                        if (!cache.containsKey(v)) {
                            var comp = compFunction.apply(v);
                            if (comp != null) {
                                var r = comp.createRegion();
                                r.setVisible(false);
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

        if (asynchronous) {
            Platform.runLater(update);
        } else {
            PlatformThread.runLaterIfNeeded(update);
        }
    }
}
