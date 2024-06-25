package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ListBoxViewComp<T> extends Comp<CompStructure<ScrollPane>> {

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even");

    private final ObservableList<T> shown;
    private final ObservableList<T> all;
    private final Function<T, Comp<?>> compFunction;
    private final int limit = Integer.MAX_VALUE;
    private final boolean scrollBar;

    public ListBoxViewComp(ObservableList<T> shown, ObservableList<T> all, Function<T, Comp<?>> compFunction, boolean scrollBar) {
        this.shown = PlatformThread.sync(shown);
        this.all = PlatformThread.sync(all);
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
        vbox.requestLayout();

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            refresh(vbox, c.getList(), all, cache, true);
        });

        all.addListener((ListChangeListener<? super T>) c -> {
            cache.keySet().retainAll(c.getList());
        });

        var scroll = new ScrollPane(vbox);
        if (scrollBar) {
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            scroll.skinProperty().subscribe(newValue -> {
                if (newValue != null) {
                    ScrollBar bar = (ScrollBar) scroll.lookup(".scroll-bar:vertical");
                    bar.opacityProperty().bind(Bindings.createDoubleBinding(() -> {
                        var v = bar.getVisibleAmount();
                        return v < 1.0 ? 1.0 : 0.0;
                    }, bar.visibleAmountProperty()));
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
            // Clear cache of unused values
            cache.keySet().removeIf(t -> !all.contains(t));

            // Create copy to reduce chances of concurrent modification
            var shownCopy = new ArrayList<>(shown);
            var newShown = shownCopy.stream()
                    .map(v -> {
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
                r.pseudoClassStateChanged(ODD, false);
                r.pseudoClassStateChanged(EVEN, false);
                r.pseudoClassStateChanged(i % 2 == 0 ? EVEN : ODD, true);
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
