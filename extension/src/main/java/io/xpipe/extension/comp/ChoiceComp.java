package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.util.PlatformUtil;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.apache.commons.collections4.BidiMap;

import java.util.function.Supplier;

public class ChoiceComp<T> extends Comp<CompStructure<ChoiceBox<T>>> {

    private final Property<T> value;
    private final BidiMap<T, Supplier<String>> range;

    public ChoiceComp(Property<T> value, BidiMap<T, Supplier<String>> range) {
        this.value = value;
        this.range = range;
    }

    @Override
    public CompStructure<ChoiceBox<T>> createBase() {
        var list = FXCollections.observableArrayList(range.keySet());
        var cb = new ChoiceBox<>(list);
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return range.get(object).get();
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });
        PlatformUtil.connect(value, cb.valueProperty());
        cb.getStyleClass().add("choice-comp");
        return new CompStructure<>(cb);
    }
}
