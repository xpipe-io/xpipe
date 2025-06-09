package io.xpipe.app.hub.comp;

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
