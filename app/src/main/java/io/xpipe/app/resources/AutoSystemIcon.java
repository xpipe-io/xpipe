package io.xpipe.app.resources;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.FailableFunction;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=true)
public class AutoSystemIcon extends SystemIcon {

    FailableFunction<ShellControl, Boolean, Exception> applicable;

    public AutoSystemIcon(String iconName, String displayName, FailableFunction<ShellControl, Boolean, Exception> applicable) {
        super(iconName, displayName);
        this.applicable = applicable;
    }

    @Override
    public boolean isApplicable(ShellControl sc) throws Exception {
        return applicable.apply(sc);
    }
}
