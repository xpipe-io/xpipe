package io.xpipe.app.comp.storage;

import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Comparator;

public class StorageFilter {

    private final StringProperty filter = new SimpleStringProperty("");

    public <T extends Filterable> void createFilterBinding(
            ObservableList<T> all, ObservableList<T> shown, ObservableValue<Comparator<T>> order) {
        all.addListener((ListChangeListener<? super T>) lc -> {
            update(all, shown, order.getValue());
        });

        SimpleChangeListener.apply(filter, n -> {
            update(all, shown, order.getValue());
        });

        order.addListener((observable, oldValue, newValue) -> {
            update(all, shown, newValue);
        });
    }

    private <T extends Filterable> void update(ObservableList<T> all, ObservableList<T> shown, Comparator<T> order) {
        var updatedShown = new ArrayList<>(shown);
        updatedShown.removeIf(e -> !all.contains(e) || !e.shouldShow(filter.get()));
        for (var e : all) {
            if (!updatedShown.contains(e) && e.shouldShow(filter.get())) {
                updatedShown.add(e);
            }
        }
        updatedShown.sort(order);
        shown.setAll(updatedShown);
    }

    public String getFilter() {
        return filter.get();
    }

    public StringProperty filterProperty() {
        return filter;
    }

    public interface Filterable {

        boolean shouldShow(String filter);
    }
}
