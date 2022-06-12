package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

@JsonTypeName("query")
@Getter
public class BaseQueryElement extends DialogElement {

    private final String description;
    private final boolean required;
    protected String value;

    @JsonCreator
    public BaseQueryElement(String description, boolean required, String value) {
        this.description = description;
        this.required = required;
        this.value = value;
    }
}
