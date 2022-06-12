package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class DialogElement {

    protected String id;

    public DialogElement() {
        this.id = UUID.randomUUID().toString();
    }

    public boolean apply(String value) {
        throw new UnsupportedOperationException();
    }

    public String getId() {
        return id;
    }
}
