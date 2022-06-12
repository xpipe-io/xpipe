package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = BaseQueryElement.class)
public class QueryElement extends BaseQueryElement {

    @JsonIgnore
    private final QueryConverter<?> converter;

    public QueryElement(String description, boolean required, String value, QueryConverter<?> converter) {
        super(description, required, value);
        this.converter = converter;
    }

    public QueryElement(String description, boolean required, Object value, QueryConverter<?> converter) {
        super(description, required, value != null ? value.toString() : null);
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
