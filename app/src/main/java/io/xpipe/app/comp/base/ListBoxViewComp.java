package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
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
    private final int limit = Integer.MAX_VALUE;
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

        refresh(vbox, shown, all, cache, false);

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            refresh(vbox, c.getList(), all, cache, true);
        });

        all.addListener((ListChangeListener<? super T>) c -> {
            synchronized (cache) {
                cache.keySet().retainAll(c.getList());
            }
        });

        var scroll = new ScrollPane(vbox);
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
        return new SimpleCompStructure<>(scroll);
    }

    private void refresh(
            VBox listView, List<? extends T> shown, List<? extends T> all, Map<T, Region> cache, boolean asynchronous) {
        Runnable update = () -> {
            synchronized (cache) {
                var set = new HashSet<>(all);
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
                            cache.put(v, comp != null ? comp.createRegion() : null);
                        }

                        return cache.get(v);
                    })
                    .filter(region -> region != null)
                    .limit(limit)
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
        };

        if (asynchronous) {
            Platform.runLater(update);
        } else {
            PlatformThread.runLaterIfNeeded(update);
        }
    }
}
