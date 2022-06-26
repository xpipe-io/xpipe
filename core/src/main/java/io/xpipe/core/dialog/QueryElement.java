package io.xpipe.core.dialog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = BaseQueryElement.class)
public class QueryElement extends BaseQueryElement {

    private final QueryConverter<?> converter;

    public <T> QueryElement(String description, boolean newLine, boolean required, boolean quiet, T value, QueryConverter<T> converter, boolean hidden) {
        super(description, newLine, required, hidden, quiet, value != null ? converter.toString(value) : null);
        this.converter = converter;
    }

    @Override
    public boolean apply(String value) {
        if (value == null && this.value != null) {
            if (isRequired() && this.value == null) {
                return false;
            }

            this.value = null;
            return true;
        }

        try {
            converter.convertFromString(value);
        } catch (Exception ex) {
            return false;
        }

        this.value = value;
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getConvertedValue() {
        return (T) converter.convertFromString(value);
    }
}
