package io.xpipe.app.comp.store;

import lombok.Value;

import java.util.Objects;

@Value
public class StoreNotes {

    String commited;
    String current;

    public boolean isCommited() {
        return Objects.equals(commited, current);
    }
}
