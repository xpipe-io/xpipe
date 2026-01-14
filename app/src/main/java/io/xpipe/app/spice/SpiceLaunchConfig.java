package io.xpipe.app.spice;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.vnc.VncBaseStore;
import io.xpipe.core.SecretValue;
import lombok.Value;

import java.nio.file.Path;
import java.util.Optional;

@Value
public class SpiceLaunchConfig {

    DataStoreEntryRef entry;
    Path file;
}
