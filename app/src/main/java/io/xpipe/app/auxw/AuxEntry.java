package io.xpipe.app.auxw;

import io.xpipe.app.storage.DataStoreColor;
import lombok.Value;

@Value
public class AuxEntry {

    String name;
    String icon;
    DataStoreColor color;
    ControllableWindowProcess process;
}
