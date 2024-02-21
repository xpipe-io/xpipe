package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonTypeName("header")
@EqualsAndHashCode(callSuper = true)
@ToString
public class HeaderElement extends DialogElement {

    protected final String header;

    @JsonCreator
    public HeaderElement(String header) {
        this.header = header;
    }

    @Override
    public String toDisplayString() {
        return header;
    }

    @Override
    public boolean apply(String value) {
        return true;
    }
}
