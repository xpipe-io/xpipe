package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonTypeName("query")
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class BaseQueryElement extends DialogElement {

    private final String description;
    @Getter
    private final boolean newLine;
    private final boolean required;
    private final boolean secret;
    private final boolean quiet;
    protected String value;

    @JsonCreator
    public BaseQueryElement(
            String description, boolean newLine, boolean required, boolean secret, boolean quiet, String value) {
        this.description = description;
        this.newLine = newLine;
        this.required = required;
        this.secret = secret;
        this.quiet = quiet;
        this.value = value;
    }

    @Override
    public boolean requiresExplicitUserInput() {
        return required && value == null;
    }

    @Override
    public String toDisplayString() {
        return description;
    }
}
