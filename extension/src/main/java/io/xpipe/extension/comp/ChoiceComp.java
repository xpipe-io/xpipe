package io.xpipe.extension.comp;

import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.store.DefaultValueStoreComp;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.apache.commons.collections4.BidiMap;

import java.util.function.Supplier;

public class ChoiceComp<T> extends DefaultValueStoreComp<CompStructure<ChoiceBox<T>>, T> {

    private final BidiMap<T, Supplier<String>> range;

    public ChoiceComp(T defaultVal, BidiMap<T, Supplier<String>> range) {
        super(defaultVal);
        this.range = range;
    }

    @Override
    protected boolean isValid(T newValue) {
        return range.containsKey(newValue);
    }

    public BidiMap<T, Supplier<String>> getRange() {
        return range;
    }

    @Override
    public CompStructure<ChoiceBox<T>> createBase() {
        var comp = this;
        var list = FXCollections.observableArrayList(comp.getRange().keySet());
        var cb = new ChoiceBox<>(list);
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return comp.getRange().get(object).get();
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });
        cb.valueProperty().bindBidirectional(comp.valueProperty());
        cb.getStyleClass().add("choice-comp");
        return new CompStructure<>(cb);
    }
}
