package io.xpipe.app.comp.base;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.util.PrettyListView;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ListViewComp<T> extends Comp<CompStructure<ListView<Node>>> {

    private final ObservableList<T> shown;
    private final ObservableList<T> all;
    private final Property<T> selected;
    private final Function<T, Comp<?>> compFunction;

    public ListViewComp(
            ObservableList<T> shown, ObservableList<T> all, Property<T> selected, Function<T, Comp<?>> compFunction) {
        this.shown = PlatformThread.sync(shown);
        this.all = PlatformThread.sync(all);
        this.selected = selected;
        this.compFunction = compFunction;
    }

    @Override
    public CompStructure<ListView<Node>> createBase() {
        Map<T, Region> cache = new HashMap<>();

        PrettyListView<Node> listView = new PrettyListView<>();
        listView.setFocusTraversable(false);
        if (selected == null) {
            listView.disableSelection();
        }

        refresh(listView, shown, cache, false);
        listView.requestLayout();

        if (selected != null) {
            if (selected.getValue() != null && shown.contains(selected.getValue())) {
                listView.getSelectionModel().select(shown.indexOf(selected.getValue()));
            }

            AtomicBoolean internalSelection = new AtomicBoolean(false);
            listView.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
                // if (true) return;

                var item = cache.entrySet().stream()
                        .filter(e -> e.getValue().equals(n))
                        .map(e -> e.getKey())
                        .findAny()
                        .orElse(null);
                internalSelection.set(true);
                selected.setValue(item);
                internalSelection.set(false);
            });

            selected.addListener((c, o, n) -> {
                if (internalSelection.get()) {
                    return;
                }

                var selectedNode = cache.get(n);
                PlatformThread.runLaterIfNeeded(() -> {
                    listView.getSelectionModel().select(selectedNode);
                });
            });
        } else {
            listView.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
                if (n != null) {
                    listView.getSelectionModel().clearSelection();
                    listView.getScene().getRoot().requestFocus();
                }
            });
        }

        shown.addListener((ListChangeListener<? super T>) (c) -> {
            refresh(listView, c.getList(), cache, true);
        });

        all.addListener((ListChangeListener<? super T>) c -> {
            cache.keySet().retainAll(c.getList());
        });

        return new SimpleCompStructure<>(listView);
    }

    private void refresh(ListView<Node> listView, List<? extends T> c, Map<T, Region> cache, boolean asynchronous) {
        Runnable update = () -> {
            var newShown = c.stream()
                    .map(v -> {
                        if (!cache.containsKey(v)) {
                            cache.put(v, compFunction.apply(v).createRegion());
                        }

                        return cache.get(v);
                    })
                    .toList();

            if (!listView.getItems().equals(newShown)) {
                BindingsHelper.setContent(listView.getItems(), newShown);
                listView.layout();
            }
        };

        if (asynchronous) {
            Platform.runLater(update);
        } else {
            PlatformThread.runLaterIfNeeded(update);
        }
    }
}
