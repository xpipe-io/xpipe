package io.xpipe.app.resources;

import io.xpipe.core.process.ShellControl;
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
}
