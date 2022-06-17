package io.xpipe.core.dialog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = BaseQueryElement.class)
public class QueryElement extends BaseQueryElement {

    private final QueryConverter<?> converter;

    public QueryElement(String description, boolean newLine, boolean required, Object value, QueryConverter<?> converter, boolean hidden) {
        super(description, newLine, required, hidden, value != null ? value.toString() : null);
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
