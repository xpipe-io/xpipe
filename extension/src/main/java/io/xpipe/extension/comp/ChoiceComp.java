package io.xpipe.extension.comp;

import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
import io.xpipe.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import lombok.Value;

import java.util.Map;

@Value
public class ChoiceComp<T> extends Comp<CompStructure<ComboBox<T>>> {

    Property<T> value;
    ObservableValue<Map<T, ObservableValue<String>>> range;

    public ChoiceComp(Property<T> value, Map<T, ObservableValue<String>> range) {
        this.value = value;
        this.range = new SimpleObjectProperty<>(range);
    }

    public ChoiceComp(Property<T> value, ObservableValue<Map<T, ObservableValue<String>>> range) {
        this.value = value;
        this.range = range;
    }

    @Override
    public CompStructure<ComboBox<T>> createBase() {
        var cb = new ComboBox<T>();
        cb.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return I18n.get("extension.none");
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
        SimpleChangeListener.apply(PlatformThread.sync(range), c -> {

            var list = FXCollections.observableArrayList(c.keySet());
            if (!list.contains(null)) {
                list.add(null);
            }
            cb.setItems(list);
        });
        PlatformThread.connect(value, cb.valueProperty());
        cb.getStyleClass().add("choice-comp");
        return new SimpleCompStructure<>(cb);
    }
}
