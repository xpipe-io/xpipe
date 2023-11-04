package io.xpipe.app.util;

import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

public interface Translatable {

    static <T extends Translatable> StringConverter<T> stringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? null : t.toTranslatedString();
            }

            @Override
            public T fromString(String string) {
                throw new AssertionError();
            }
        };
    }

    static <T extends Translatable> StringExpression asTranslatedString(ObservableValue<T> observableValue) {
        return new StringBinding() {
            {
                super.bind(observableValue);
            }

            @Override
            public void dispose() {
                super.unbind(observableValue);
            }

            @Override
            public ObservableList<ObservableValue<?>> getDependencies() {
                return FXCollections.singletonObservableList(observableValue);
            }

            @Override
            protected String computeValue() {
                final T value = observableValue.getValue();
                return (value == null) ? "null" : value.toTranslatedString();
            }
        };
    }

    String toTranslatedString();
}
