package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;

@JsonTypeName("query")
@Getter
public class BaseQueryElement extends DialogElement {

    private final String description;
    private final boolean newLine;
    private final boolean required;
    private final boolean hidden;
    protected String value;

    @JsonCreator
    public BaseQueryElement(String description, boolean newLine, boolean required, boolean hidden, String value) {
        this.description = description;
        this.newLine = newLine;
        this.required = required;
        this.hidden = hidden;
        this.value = value;
    }

    public boolean isNewLine() {
        return newLine;
    }
}
