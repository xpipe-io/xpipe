package io.xpipe.extension.comp;

import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.apache.commons.collections4.BidiMap;

public class ChoiceComp<T> extends Comp<CompStructure<ComboBox<T>>> {

    private final Property<T> value;
    private final BidiMap<T, ObservableValue<String>> range;

    public ChoiceComp(Property<T> value, BidiMap<T, ObservableValue<String>> range) {
        this.value = value;
        this.range = range;
    }

    @Override
    public CompStructure<ComboBox<T>> createBase() {
        var list = FXCollections.observableArrayList(range.keySet());
        var cb = new ComboBox<>(list);
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return I18n.get("extension.none");
                }

                return range.get(object).getValue();
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });
        PlatformThread.connect(value, cb.valueProperty());
        cb.getStyleClass().add("choice-comp");
        return new SimpleCompStructure<>(cb);
    }
}
