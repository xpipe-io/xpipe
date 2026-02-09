package io.xpipe.app.spice;

import io.xpipe.app.storage.DataStoreEntry;

import lombok.Value;

import java.nio.file.Path;

@Value
public class SpiceLaunchConfig {

    DataStoreEntry entry;
    Path file;
}
