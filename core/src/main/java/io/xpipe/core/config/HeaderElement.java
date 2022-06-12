package io.xpipe.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("header")
public class HeaderElement extends DialogElement {

    protected String header;

    @JsonCreator
    public HeaderElement(String header) {
        this.header = header;
    }

    @Override
    public boolean apply(String value) {
        return true;
    }

    public String getHeader() {
        return header;
    }
}
