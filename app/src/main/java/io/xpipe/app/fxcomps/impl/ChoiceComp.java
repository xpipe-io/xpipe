package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChoiceComp<T> extends Comp<CompStructure<ComboBox<T>>> {

    Property<T> value;
    ObservableValue<Map<T, ObservableValue<String>>> range;
    boolean includeNone;

    public ChoiceComp(Property<T> value, Map<T, ObservableValue<String>> range, boolean includeNone) {
        this.value = value;
        this.range = new SimpleObjectProperty<>(range);
        this.includeNone = includeNone;
    }

    public ChoiceComp(Property<T> value, ObservableValue<Map<T, ObservableValue<String>>> range, boolean includeNone) {
        this.value = value;
        this.range = PlatformThread.sync(range);
        this.includeNone = includeNone;
    }

    @Override
    public CompStructure<ComboBox<T>> createBase() {
        var cb = new ComboBox<T>();
        cb.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return AppI18n.get("app.none");
                }

                var found = range.getValue().get(object);
                if (found == null) {
                    return "";
                }

                return found.getValue();
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });
        SimpleChangeListener.apply(range, c -> {
            var list = FXCollections.observableArrayList(c.keySet());
            if (!list.contains(null) && includeNone) {
                list.add(null);
            }

            BindingsHelper.setContent(cb.getItems(), list);
        });

        cb.valueProperty().addListener((observable, oldValue, newValue) -> {
            value.setValue(newValue);
        });
        SimpleChangeListener.apply(value, val -> {
            PlatformThread.runLaterIfNeeded(() -> cb.valueProperty().set(val));
        });

        cb.getStyleClass().add("choice-comp");
        return new SimpleCompStructure<>(cb);
    }
}
