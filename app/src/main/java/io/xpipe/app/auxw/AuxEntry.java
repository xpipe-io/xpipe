package io.xpipe.app.auxw;

import lombok.Value;

@Value
public class AuxEntry {

    String name;
    ControllableWindowProcess process;
}
