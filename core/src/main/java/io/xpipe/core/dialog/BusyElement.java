package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("busy")
public class BusyElement extends DialogElement {

    @Override
    public boolean apply(String value) {
        return true;
    }
}
