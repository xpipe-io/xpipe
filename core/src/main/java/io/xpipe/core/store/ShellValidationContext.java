package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import lombok.Value;

@Value
public class ShellValidationContext implements ValidationContext<ShellControl> {

    ShellControl shellControl;

    @Override
    public ShellControl get() {
        return shellControl;
    }

    @Override
    public void close() {
        try {
            shellControl.shutdown();
        } catch (Exception ignored) {
        }
    }
}
