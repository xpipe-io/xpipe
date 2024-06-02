package io.xpipe.app.beacon;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import lombok.Value;

@Value
public class BeaconShellSession {

    DataStoreEntry entry;
    ShellControl control;
}
