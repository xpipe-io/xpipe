package io.xpipe.app.resources;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class SystemIcon {

    String iconName;
    String displayName;

    public boolean isApplicable(ShellControl sc) throws Exception {
        return false;
    }

    public boolean isApplicable(DataStore store) {
        return false;
    }
}
