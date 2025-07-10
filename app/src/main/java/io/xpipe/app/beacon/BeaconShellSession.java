package io.xpipe.app.beacon;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntry;

import lombok.Value;

@Value
public class BeaconShellSession {

    DataStoreEntry entry;
    ShellControl control;
}
