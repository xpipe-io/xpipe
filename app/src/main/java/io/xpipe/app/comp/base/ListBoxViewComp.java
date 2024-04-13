package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.ListBindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
    private int limit = Integer.MAX_VALUE;

    public ListBoxViewComp(ObservableList<T> shown, ObservableList<T> all, Function<T, Comp<?>> compFunction) {
        this.shown = PlatformThread.sync(shown);
        this.all = PlatformThread.sync(all);
        this.compFunction = compFunction;
    }

    @Override
    public CompStructure<ScrollPane> createBase() {
        Map<T, Region> cache = new IdentityHashMap<>();

        VBox vbox = new VBox();
        vbox.getStyleClass().add("content");
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

            var newShown = shown.stream()
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

            for (int i = 0; i < newShown.size(); i++) {
                var r = newShown.get(i);
                r.pseudoClassStateChanged(ODD, false);
                r.pseudoClassStateChanged(EVEN, false);
                r.pseudoClassStateChanged(i % 2 == 0 ? EVEN : ODD, true);
            }

            if (!listView.getChildren().equals(newShown)) {
                ListBindingsHelper.setContent(listView.getChildren(), newShown);
            }
        };

        if (asynchronous) {
            Platform.runLater(update);
        } else {
            PlatformThread.runLaterIfNeeded(update);
        }
    }
}
