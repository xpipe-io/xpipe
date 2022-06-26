package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonTypeName("busy")
@EqualsAndHashCode(callSuper = true)
@ToString
public class BusyElement extends DialogElement {

    @Override
    public String toDisplayString() {
        return "busy";
    }

    @Override
    public boolean apply(String value) {
        return true;
    }
}
