package io.xpipe.app.resources;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
public class SystemIcon {

    SystemIconSource source;
    String iconName;
    String displayName;
}
